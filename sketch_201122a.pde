import static javax.swing.JOptionPane.*;

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



void settings(){
  size(N*SCALE,N*SCALE);
}


void setup(){
  frameRate(FrameRateCap);
  InitUI();
  
  fluids = loadJSONArray("data/fluids.json");
  GetFluid(choice);
}


void RenderUI(){
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

void InitUI(){  
  String message = "The key bindings for the simulation are as follows:\nd : Toggle Dark Mode";
  message += "\nc : Toggle Color Coding\nn : Clear\nf : Cycle Through Fluids";
  message += "\nDrag Left Click : Add Fluid\nDrag Right Click : Remove Fluid\nScroll : Change Brush Size";
  message += "\ns : Lower Simulation Speed\na : Increase Simulation Speed";
  message += "\n\nThe legend of the UI is:\nFPS (" + str(FrameRateCap) + " max)  /  Brush Size /  Speed of Simulation  /  Fluid Configuration";
  
  showMessageDialog(null, message, "UI Help", INFORMATION_MESSAGE);
  
  
}


int ConstrainMouse(int maxV, int val){
  if(val >= maxV){
      return maxV - 1;
  }
  if(val<=0){
     return 1; 
  }
  return val;
}

void GetFluid(int newchoice){
  choice = newchoice%fluids.size();
  JSONObject fluids_choice = fluids.getJSONObject(choice);
  
  FluidName = fluids_choice.getString("configuration");
  FluidDiffusion = fluids_choice.getFloat("diffusion");
  FluidViscosity = fluids_choice.getFloat("viscosity");
  
  fluid = new Fluid(FluidDiffusion*DSCALE, FluidViscosity*DSCALE, dt);
}


void SetSimulationSpeed(boolean increase){
  if(increase){
    dt+=0.002;
    if(dt>max_dt){dt=max_dt;}
  }
  else{
    dt-=0.002;
    if(dt<min_dt){dt=min_dt;}
  }
  GetFluid(choice);
} //<>//

void mouseDragged(){
    
   float amountX = mouseX - pmouseX;
   float amountY = mouseY - pmouseY;
   
   int maxX = int(width/SCALE) - 1;
   int maxY = int(height/SCALE) - 1;
   
   if (mousePressed && (mouseButton == LEFT)){
     fluid.AddDensity(ConstrainMouse(maxX,mouseX/SCALE) , ConstrainMouse(maxY, mouseY/SCALE) , brush_size);
     fluid.AddVelocity(ConstrainMouse(maxX,mouseX/SCALE) , ConstrainMouse(maxY, mouseY/SCALE) , amountX*10, amountY*10);
   }
   if(mousePressed && (mouseButton == RIGHT)){
     fluid.AddDensity(ConstrainMouse(maxX,mouseX/SCALE) , ConstrainMouse(maxY, mouseY/SCALE), -brush_size);
   }

}


void keyPressed(){
  if(key=='d'){dark = !dark;}
  if(key=='c'){colorcode = !colorcode;}
  if(key=='n'){GetFluid(choice);}
  if(key=='f'){GetFluid(choice+1);}
  if(key=='a'){SetSimulationSpeed(true);}
  if(key=='s'){SetSimulationSpeed(false);}
}
  
void mouseWheel(MouseEvent event){
   float e = -1*event.getCount();
   brush_size += e*20;
   if(brush_size <= min_brush_size){brush_size=min_brush_size;}
   if(brush_size>=max_brush_size){brush_size=max_brush_size;}
}

void draw(){
  background(0);
  
  fluid.step(dt);
  fluid.RenderD(dark,colorcode);
  
  RenderUI();
  
}
