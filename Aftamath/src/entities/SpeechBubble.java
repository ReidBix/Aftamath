package entities;

import static handlers.Vars.PPM;
import handlers.Vars;
import main.Game;
import main.GameState;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;

public class SpeechBubble extends Entity {

	public int sizingState;
	public boolean expanded;

	private Entity owner;
	private Vector2 range = new Vector2(2.5f/PPM, 2.5f/PPM);
	private Vector2 goal;
	private Vector2 center, v, dc;
	private String message;
	private TextureRegion[] font;
	private TextureRegion left, middle, right;
	private boolean reached = true;
	private int idleTime = 30;
	private int time = idleTime;
	private int maxWidth = 16, minWidth, innerWidth, positioningType;
	private float px, py;
	
	public static final int DEFAULT_WIDTH = 14;
	public static final int DEFAULT_HEIGHT = 12;
	public static final int EXPANDING = 1;
	public static final int COLLAPSING = -1;
	public static final int LEFT_MARGIN = 0;
	public static final int CENTERED = 1;
	public static final int RIGHT_MARGIN = 2;
	
	/**
	 * Types:
	 * 1 - ...;
	 * 2 - exciting;
	 * 3 - curious;
	 * 4 - cowardly;
	 * 5 - power;
	 * 6 - Yes;
	 * 7 - No
	 */
		
	
	//Standard interaction based bubble, e.g. speech
	public SpeechBubble(Entity d, float x, float y, int ID, String message, int positioningType) {
		super(x, y, 14, 12, "speechBubble");
		setPlayState(d.getPlayState());
		gs.addObject(this);
		this.ID += ID;
		this.message = message;
		this.positioningType = positioningType;
		owner = d;
		player = d.getPlayer();
		center = new Vector2(x/PPM, y/PPM);
		v = new Vector2(center.x - owner.getPosition().x, center.y - owner.getPosition().y);

		font = TextureRegion.split(new Texture(Gdx.files.internal("res/images/text5.png")), 7, 9 )[0];
		left = new TextureRegion(Game.res.getTexture("speechBubble"), 10, 96, 3, 12);
		middle = new TextureRegion(Game.res.getTexture("speechBubble"), 14, 96, 1, 12);
		right = new TextureRegion(Game.res.getTexture("speechBubble"), 29, 96, 3, 12);
		
		maxWidth = message.length() * font[0].getRegionWidth();
		TextureRegion[] sprites = TextureRegion.split(Game.res.getTexture("speechBubble"), width, height)[ID];
		setDefaultAnimation(sprites[sprites.length - 1]);
		animation.setAction(sprites, sprites.length, false, 1, Vars.ACTION_ANIMATION_RATE);
	}
	
	public SpeechBubble(Entity d, float x, float y, String ID){
		super(x, y, getWidth(ID), getHeight(ID), ID);
		setPlayState(d.getPlayState());
		gs.addObject(this);
		this.message = "";
		owner = d;
		player = d.getPlayer();
		center = new Vector2(x/PPM, y/PPM);
		v = new Vector2(center.x - owner.getPosition().x, center.y - owner.getPosition().y);

		font = TextureRegion.split(new Texture(Gdx.files.internal("res/images/text5.png")), 7, 9 )[0];
		TextureRegion[] sprites = TextureRegion.split(Game.res.getTexture(ID), width, height)[0];
		setDefaultAnimation(sprites, Vars.ACTION_ANIMATION_RATE*2);
		animation.setAction(TextureRegion.split(texture, width, height)[1], determineLength(ID), 
				false, 1, Vars.ACTION_ANIMATION_RATE/2);
	}

	public void update(float dt){
		animation.update(dt);
		
		//calculations for hovering
		if (body != null) {
			reposition();
			Vector2 tmp = center.cpy();
			center = new Vector2(owner.getPosition().x + v.x, owner.getPosition().y + v.y);
			dc = new Vector2(center.x - tmp.x, center.y - tmp.y);
			if(goal != null) goal = new Vector2(goal.x + dc.x, goal.y + dc.y);
		}
		
		//show or hide internal message
		int diff = maxWidth/8;
		if(sizingState == EXPANDING){
			if(innerWidth >= maxWidth){ 
				sizingState = 0;
				expanded = true;
				innerWidth = maxWidth;
			} else 
				innerWidth+=diff;
		} if (sizingState == COLLAPSING){
			if(innerWidth <= minWidth)
				sizingState = 0;
			else
				innerWidth-=diff;
		}
		
		//destroy object if interaction has lost contact
		if(body != null && player.getInteractable() != owner && ID.equals("speechBubble0")) 
			gs.addBodyToRemove(body);
	}
	
	public void render(SpriteBatch sb){
		if(sizingState!=0||expanded){
			switch(positioningType){
				case LEFT_MARGIN:
					sb.draw(left, getPosition().x*Vars.PPM - rw, getPosition().y*Vars.PPM-rh);
					sb.draw(right, getPosition().x*Vars.PPM - rw + innerWidth +3, getPosition().y*Vars.PPM-rh);
					for (int i = 0; i<innerWidth;i++)
						sb.draw(middle, getPosition().x*Vars.PPM - rw + i+3, getPosition().y*Vars.PPM-rh);
					break;
				case CENTERED:
					sb.draw(left, getPosition().x*Vars.PPM - rw - innerWidth/2 + 1, getPosition().y*Vars.PPM-rh);
					sb.draw(right, getPosition().x*Vars.PPM - rw + innerWidth/2 +3, getPosition().y*Vars.PPM-rh);
					for (int i = 0; i<innerWidth/2;i++){
						sb.draw(middle, getPosition().x*Vars.PPM - rw + i+3, getPosition().y*Vars.PPM-rh);
						sb.draw(middle, getPosition().x*Vars.PPM - rw - i+3, getPosition().y*Vars.PPM-rh);
					}
					break;
				case RIGHT_MARGIN:
					sb.draw(left, getPosition().x*Vars.PPM + rw - innerWidth - 5, getPosition().y*Vars.PPM-rh);
					sb.draw(right, getPosition().x*Vars.PPM + rw - 3, getPosition().y*Vars.PPM-rh);
					for (int i = 0; i<innerWidth;i++)
						sb.draw(middle, getPosition().x*Vars.PPM + rw - i-3, getPosition().y*Vars.PPM-rh);
					break;
				}
			if(expanded){
				float x = 0;
				if (positioningType==RIGHT_MARGIN) x = message.length()*font[0].getRegionWidth()- font[0].getRegionWidth()-2;
				else if (positioningType==CENTERED) x = message.length()*font[0].getRegionWidth()/2f-1;
				GameState.drawString(sb, font, font[0].getRegionWidth(), message, 
						getPosition().x*Vars.PPM-rw-x+3, getPosition().y*Vars.PPM-rh+2f);
			}
		} else
			super.render(sb);
	}
	
	public void expand(){ 
		if(!ID.startsWith("speechBubble")) return;
		sizingState = EXPANDING; 
	}
	
	public void collapse(){ 
		if(!ID.startsWith("speechBubble")) return;
		sizingState = COLLAPSING; 
		expanded = false;
	}
	
	public void reposition() {
		time++;
		if (reached && time >= idleTime){
			reached = false;

			goal = new Vector2((float)((Math.random() * range.x * 2) - range.x + center.x), 
					(float)((Math.random() * range.y * 2) - range.y + center.y));
		}

		if (!reached){
			float dx = goal.x - body.getPosition().x; float dy = goal.y - body.getPosition().y;
			if (Math.abs(dx) <= 0.01 && Math.abs(dy) <= 0.01){
				reached = true;
				time = 0;
			}
			else {
				px = dx * 1.5f; py = dy * 1.5f;
				body.setLinearVelocity(px, py);
			}
		}
	}
	
	public void create(){
		//hitbox
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw-2)/PPM, (rh)/PPM);
		
		bdef.position.set(x/PPM, y/PPM);
		bdef.type = BodyType.KinematicBody;
		fdef.shape = shape;
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) (layer | Vars.BIT_GROUND | Vars.BIT_PROJECTILE);
		fdef.filter.categoryBits = layer;
		body.createFixture(fdef).setUserData(Vars.trimNumbers(ID));
	}
	
	private static int getWidth(String ID){
		try{
			Texture src = new Texture(Gdx.files.internal("res/images/entities/"+ID+"base.png"));
			return src.getWidth();
		} catch(Exception e) {
			return DEFAULT_WIDTH;
		}
	}

	private static int getHeight(String ID){
		try{
			Texture src = new Texture(Gdx.files.internal("res/images/entities/"+ID+"base.png"));
			return src.getHeight();
		} catch(Exception e) {
			return DEFAULT_HEIGHT;
		}
	}
	
	private static int determineLength(String ID){
		switch(ID){
		case "arrow":
			return 3;
		}
		return 1;
	}

	public Entity getOwner() { return owner;}
	public String getMessage(){ return message; }
	public void setMessage(String message){ this.message = message; }
}
