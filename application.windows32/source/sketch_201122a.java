import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import static javax.swing.JOptionPane.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class sketch_201122a extends PApplet {



Fluid fluid;
JSONArray fluids;

int choice = 0;
String FluidName;
Float FluidDiffusion, FluidViscosity;
int brush_size = 1000;
final int FrameRateCap=120;

final int max_brush_size = 4000;
final int min_brush_size = 100;
boolean dark = true;
boolean colorcode = true;

int DSCALE = 100;



public void settings(){
  size(N*SCALE,N*SCALE);
}


public void setup(){
  frameRate(FrameRateCap);
  InitUI();
  
  fluids = loadJSONArray("data/fluids.json");
  GetFluid(choice);
}


public void RenderUI(){
  textSize(32);
  colorMode(RGB);
  textMode(MODEL);
  if(dark){fill(255,255,255);}
  else{fill(0,0,0);}
  String message;
  if(brush_size<1000){message = "FPS: " + str(round(frameRate)) + " / Brush: 0" + str(brush_size) + " / Speed: " + String.format("%.3f", dt) + " / " + FluidName;}
  else{message = "FPS: " + str(round(frameRate)) + " / Brush: " + str(brush_size) + " / Speed: " + String.format("%.3f", dt) + " / " + FluidName;}
  text(message,0,25);
}

public void InitUI(){  
  String message = "The key bindings for the simulation are as follows:\nd : Toggle Dark Mode";
  message += "\nc : Toggle Color Coding\nn : Clear\nf : Cycle Through Fluids";
  message += "\nDrag Left Click : Add Fluid\nDrag Right Click : Remove Fluid\nScroll : Change Brush Size";
  message += "\ns : Lower Simulation Speed\na : Increase Simulation Speed";
  message += "\n\nThe legend of the UI is:\nFPS (" + str(FrameRateCap) + " max)  /  Brush Size /  Speed of Simulation  /  Fluid Configuration";
  
  showMessageDialog(null, message, "UI Help", INFORMATION_MESSAGE);
  
  
}


public int ConstrainMouse(int maxV, int val){
  if(val >= maxV){
      return maxV - 1;
  }
  if(val<=0){
     return 1; 
  }
  return val;
}

public void GetFluid(int newchoice){
  choice = newchoice%fluids.size();
  JSONObject fluids_choice = fluids.getJSONObject(choice);
  
  FluidName = fluids_choice.getString("configuration");
  FluidDiffusion = fluids_choice.getFloat("diffusion");
  FluidViscosity = fluids_choice.getFloat("viscosity");
  
  fluid = new Fluid(FluidDiffusion*DSCALE, FluidViscosity*DSCALE, dt);
}


public void SetSimulationSpeed(boolean increase){
  if(increase){
    dt+=0.002f;
    if(dt>max_dt){dt=max_dt;}
  }
  else{
    dt-=0.002f;
    if(dt<min_dt){dt=min_dt;}
  }
  GetFluid(choice);
}

public void mouseDragged(){
    
   float amountX = mouseX - pmouseX;
   float amountY = mouseY - pmouseY;
   
   int maxX = PApplet.parseInt(width/SCALE) - 1;
   int maxY = PApplet.parseInt(height/SCALE) - 1;
   
   if (mousePressed && (mouseButton == LEFT)){
     fluid.AddDensity(ConstrainMouse(maxX,mouseX/SCALE) , ConstrainMouse(maxY, mouseY/SCALE) , brush_size);
     fluid.AddVelocity(ConstrainMouse(maxX,mouseX/SCALE) , ConstrainMouse(maxY, mouseY/SCALE) , amountX*10, amountY*10);
   }
   if(mousePressed && (mouseButton == RIGHT)){
     fluid.AddDensity(ConstrainMouse(maxX,mouseX/SCALE) , ConstrainMouse(maxY, mouseY/SCALE), -brush_size);
   }

}


public void keyPressed(){
  if(key=='d'){dark = !dark;}
  if(key=='c'){colorcode = !colorcode;}
  if(key=='n'){GetFluid(choice);}
  if(key=='f'){GetFluid(choice+1);}
  if(key=='a'){SetSimulationSpeed(true);}
  if(key=='s'){SetSimulationSpeed(false);}
}
  
public void mouseWheel(MouseEvent event){
   float e = -1*event.getCount();
   brush_size += e*20;
   if(brush_size <= min_brush_size){brush_size=min_brush_size;}
   if(brush_size>=max_brush_size){brush_size=max_brush_size;}
}

public void draw(){
  background(0);
  
  fluid.step(dt);
  fluid.RenderD(dark,colorcode);
  
  RenderUI();
  
}
final int N=64;
final int GRIDSIZE = (N+2)*(N+2);
final int SCALE = 15;

float dt = 0.04f;
final float max_dt = 0.1f;
final float min_dt = 0.002f;

public int IX(int i, int j){
   return i + (N+2)*j; 
}

class Fluid{

    float dt;
    float diff;
    float visc;
    int size;
  
    float[] u;
    float[] v;
    float[] u_prev;
    float[] v_prev;
    
    float[] dens;
    float[] dens_prev;

  Fluid(float diffusion, float viscosity, float dtime){
     this.diff = diffusion;
     this.visc = viscosity;
     this.dt = dtime;
     
     this.dens_prev = new float[GRIDSIZE];
     this.dens = new float[GRIDSIZE];
     
     this.u = new float[GRIDSIZE];
     this.u_prev = new float[GRIDSIZE];
     this.v = new float[GRIDSIZE];
     this.v_prev = new float[GRIDSIZE];
  }
  
  public void step(float dtime){
     VelStep(u,v,u_prev,v_prev,visc,dtime);
     DensStep(dens,dens_prev,u_prev,v_prev,diff);
  }
  
  public void SetBnd(int b, float[] x){
    
    for (int i=1 ; i<=N ; i++ ) {
        x[IX(0 ,i)] = (b==1) ? -x[IX(1,i)] : x[IX(1,i)];
        x[IX(N+1,i)] = (b==1) ? -x[IX(N,i)] : x[IX(N,i)];
        x[IX(i,0 )] = (b==2) ? -x[IX(i,1)] : x[IX(i,1)];
        x[IX(i,N+1)] = (b==2) ? -x[IX(i,N)] : x[IX(i,N)];
    }
    x[IX(0 ,0 )] = 0.5f*(x[IX(1,0 )]+x[IX(0 ,1)]);
    x[IX(0 ,N+1)] = 0.5f*(x[IX(1,N+1)]+x[IX(0 ,N )]);
    x[IX(N+1,0 )] = 0.5f*(x[IX(N,0 )]+x[IX(N+1,1)]);
    x[IX(N+1,N+1)] = 0.5f*(x[IX(N,N+1)]+x[IX(N+1,N )]);
  }
  
  public void Advect(int b, float[] d, float[] d0, float[] u, float[] v){

    int i, j, i0, j0, i1, j1;
    float x, y, s0, t0, s1, t1, dt0;

    dt0 = dt*N;

    for(i=1;i<=N;i++){
        for(j=1;j<=N;j++){
            x = i-dt0*u[IX(i,j)]; y = j-dt0*v[IX(i,j)];
            if (x<0.5f) x=0.5f; if (x>N+0.5f) x=N+ 0.5f; i0=PApplet.parseInt(x); i1=i0+1;
            if (y<0.5f) y=0.5f; if (y>N+0.5f) y=N+ 0.5f; j0=PApplet.parseInt(y); j1=j0+1;
            s1 = x-i0; s0 = 1-s1; t1 = y-j0; t0 = 1-t1;
            d[IX(i,j)] = s0*(t0*d0[IX(i0,j0)]+t1*d0[IX(i0,j1)])+ s1*(t0*d0[IX(i1,j0)]+t1*d0[IX(i1,j1)]);


        }
    }
    SetBnd(b,d);

  }
  
  public void Diffuse(int b, float[] x, float[] x0, float diff){

    float a = dt*diff*N*N;

    for(int k=0;k<20;k++){
        for(int i=1;i<=N;i++){
            for(int j=1;j<=N;j++){
                x[IX(i,j)] = (x0[IX(i,j)] + a*(x[IX(i-1,j)] + x[IX(i+1,j)] + x[IX(i,j-1)] + x[IX(i,j+1)]))/(1+4*a);
            }
        }
        SetBnd(b,x);
    }

  }
  
  
  public void AddDensity(int x, int y, float amount){
     int index = IX(x,y);
     this.dens[index] += amount;
  }
  
  public void AddVelocity(int x, int y, float amountX, float amountY){
     int index = IX(x,y);
     this.v[index] += amountX;
     this.u[index] += amountY;
  }

  public void Project(float[] u, float[] v, float[] p, float[] div){

    int i,j,k;
    float h;

    h = 1.0f/N;
    for(i=1;i<=N;i++){
        for(j=1;j<=N;j++){
            div[IX(i,j)] = -0.5f*h*(u[IX(i+1,j)]-u[IX(i-1,j)]+ v[IX(i,j+1)]-v[IX(i,j-1)]);
            p[IX(i,j)] = 0;
        }
    }
    SetBnd(0,div); SetBnd(0,p);

    for ( k=0 ; k<20 ; k++ ) {
        for ( i=1 ; i<=N ; i++ ) {
            for ( j=1 ; j<=N ; j++ ) {
                p[IX(i,j)] = (div[IX(i,j)]+p[IX(i-1,j)]+p[IX(i+1,j)]+ p[IX(i,j-1)]+p[IX(i,j+1)])/4;
            }
        }
        SetBnd(0, p);
    }

    for ( i=1 ; i<=N ; i++ ) {
        for ( j=1 ; j<=N ; j++ ) {
            u[IX(i,j)] -= 0.5f*(p[IX(i+1,j)]-p[IX(i-1,j)])/h;
            v[IX(i,j)] -= 0.5f*(p[IX(i,j+1)]-p[IX(i,j-1)])/h;
        }
    }
    SetBnd(1, u); SetBnd(2, v);

  }
  public void DensStep (float[] x, float[] x0, float[] u, float[] v, float diff ){
    Diffuse ( 0, x0, x, diff);
    Advect ( 0, x, x0, u, v);
  }

  public void VelStep(float[] u, float[] v, float[] u0, float[] v0, float visc, float dt){
    float[] tmp;
    
    
    tmp = u0; u0 = u; u=tmp;
    Diffuse(1,u,u0,visc);
    
    tmp = v0; v0 = v; v=tmp;
    Diffuse(2,v,v0,visc);

    Project(u,v,u0,v0);
    tmp = u0; u0 = u; u=tmp;
    tmp = v0; v0 = v; v=tmp;
    
    Advect(1,u,u0,u0,v0); Advect(2,v,v0,u0,v0);
    
    Project(u,v,u0,v0);

  }

  public void RenderD(boolean dark, boolean code){
    noStroke();
    colorMode(HSB,100);
    
    for(int i=0;i<N;i++){
      for(int j=0;j<N;j++){
        int x = i * SCALE;
        int y = j * SCALE;
        
        float dr = this.dens[IX(i,j)];
        if(dark == true && code == true){fill(dr,255,dr);}
        else if (dark == false && code == false){fill(255,dr,255);}
        else if (dark == true && code == false){fill(0,dr,dr);}
        else if (dark == false && code == true){fill(dr,dr,255);}
        
        square(x,y,SCALE);
       }
     }
    
    
  }
 
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "sketch_201122a" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
