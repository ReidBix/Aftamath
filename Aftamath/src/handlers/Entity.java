package handlers;

import static handlers.Vars.PPM;
import main.Game;
import main.Play;
import scenes.Script;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import entities.Player;

public class Entity{
	
	protected World world;
	protected Play gs;
	protected Player player;
	protected Body body;
	protected Script script;
	protected final BodyDef bdef = new BodyDef();
	protected final FixtureDef fdef = new FixtureDef();
	protected short layer;
	
	public boolean isInteractable;
	protected String ID;
	protected int sceneID;
	
	public Animation animation;
	public boolean controlled;
	public float x;
	public float y;
	public int height, width, rw, rh, controlledAction;
	protected int actionTypes;
	protected Vector2 goal;
	protected Texture texture;
	protected boolean direction;
	protected static final float MAX_DISTANCE = 50; 
	
	protected Entity (float x2, float y2, int w, int h, String ID) {
		this.ID = ID;
		this.x = x2;
		this.y = y2;
		
		this.width = w; 
		this.height = h;
		
		//units in pixels, measures radius of image
		this.rw = w/2; 
		this.rh = h/2;
		
		loadSprite();
	}
	
	protected void loadSprite() {
		animation = new Animation();
		texture = Game.res.getTexture(ID);
		
		if (texture != null){
			TextureRegion[] sprites = TextureRegion.split(texture, width, height)[0];
			setDefaultAnimation(sprites);
		}
	}

	public Entity(){}
	
	public void update(float dt){
		animation.update(dt);
	}
	
	public void render(SpriteBatch sb) {
		sb.draw(animation.getFrame(), body.getPosition().x * PPM - rw, body.getPosition().y * PPM - rh);
	}
	
	public void doAction(int action){
		controlled = false;	
	}
	
	public void setDefaultAnimation(TextureRegion[] reg){
		animation.setFrames(reg, direction);
	}
	
	public void setPlayState(Play gs){
		this.gs = gs;
		setPlayer(gs.player);
		this.world = gs.getWorld();
		if (script != null) script.setPlayState(gs);
	}
	
	public Play getPlayState(){return gs;}
	public void setScript(String ID){ 
		script = new Script(ID, gs, player, this);
		isInteractable = true;
	}
	
	public void setPlayer(Player player){this.player = player;}
	public Player getPlayer(){return player; }
	
	public void changeLayer(short layer){
		this.layer = layer;
		Filter filter = body.getFixtureList().first().getFilterData();
		filter.maskBits = (short) (layer | Vars.BIT_GROUND | Vars.BIT_PROJECTILE);
		filter.categoryBits = layer;
	}
	
	public short getLayer(){ return layer; }
	
	public String getID() { return ID; }
	public int getSceneID(){ return sceneID; }
	public void setSceneID(int ID){ sceneID = ID; }
	public void setPosition(Vector2 location){
		x=location.x;
		y=location.y;
	}
	public Vector2 getPosition(){ return body.getPosition(); }
	public Body getBody() { return body; }

	public void setGoal(float gx) { 
		this.goal = new Vector2((float) gx/PPM + getPosition().x, getPosition().y); 
		doAction(1);
	}

	public Script getScript() { return script; }
	
	public String toString(){ return getID(); }
	
	public void changeDirection(){
		if (direction) direction = false;
		else direction = true;

		animation.flip(direction);
	}

	public void facePlayer(){
		float dx = player.getPosition().x - getPosition().x;
		if(dx > 0 && direction) changeDirection();
		else if (dx < 0 && !direction) changeDirection();
	}
	
	public void faceObject(Entity obj){
		if (obj != null)
			if(obj.getBody() != null){
				float dx = obj.getPosition().x - getPosition().x;
				if(dx > 0 && direction) changeDirection();
				else if (dx < 0 && !direction) changeDirection();
			}
	}
	
	public boolean getDirection(){ return direction; }

	public int compareTo(Entity e){
		if (layer < e.layer) return 1;
		if (layer > e.layer) return -1;
		return 0;
	}
	
	public void create(){
		//hitbox
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw-2)/PPM, (rh)/PPM);
		
		bdef.position.set(x/PPM, y/PPM);
		bdef.type = BodyType.DynamicBody;
		fdef.shape = shape;
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) (layer | Vars.BIT_GROUND | Vars.BIT_PROJECTILE);
		fdef.filter.categoryBits = layer;
		body.createFixture(fdef).setUserData(Vars.trimNumbers(getID()));
	}
}