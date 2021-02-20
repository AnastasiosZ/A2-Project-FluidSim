final int N=64;
final int GRIDSIZE = (N+2)*(N+2);
final int SCALE = 15;

float dt = 0.04;
final float max_dt = 0.1;
final float min_dt = 0.002;

int IX(int i, int j){
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
  
  void step(float dtime){
     VelStep(u,v,u_prev,v_prev,visc,dtime);
     DensStep(dens,dens_prev,u_prev,v_prev,diff);
  }
  
  void SetBnd(int b, float[] x){
    
    for (int i=1 ; i<=N ; i++ ) {
        x[IX(0 ,i)] = (b==1) ? -x[IX(1,i)] : x[IX(1,i)];
        x[IX(N+1,i)] = (b==1) ? -x[IX(N,i)] : x[IX(N,i)];
        x[IX(i,0 )] = (b==2) ? -x[IX(i,1)] : x[IX(i,1)];
        x[IX(i,N+1)] = (b==2) ? -x[IX(i,N)] : x[IX(i,N)];
    }
    x[IX(0 ,0 )] = 0.5*(x[IX(1,0 )]+x[IX(0 ,1)]);
    x[IX(0 ,N+1)] = 0.5*(x[IX(1,N+1)]+x[IX(0 ,N )]);
    x[IX(N+1,0 )] = 0.5*(x[IX(N,0 )]+x[IX(N+1,1)]);
    x[IX(N+1,N+1)] = 0.5*(x[IX(N,N+1)]+x[IX(N+1,N )]);
  }
  
  void Advect(int b, float[] d, float[] d0, float[] u, float[] v){

    int i, j, i0, j0, i1, j1;
    float x, y, s0, t0, s1, t1, dt0;

    dt0 = dt*N;

    for(i=1;i<=N;i++){
        for(j=1;j<=N;j++){
            x = i-dt0*u[IX(i,j)]; y = j-dt0*v[IX(i,j)];
            if (x<0.5) x=0.5; if (x>N+0.5) x=N+ 0.5; i0=int(x); i1=i0+1;
            if (y<0.5) y=0.5; if (y>N+0.5) y=N+ 0.5; j0=int(y); j1=j0+1;
            s1 = x-i0; s0 = 1-s1; t1 = y-j0; t0 = 1-t1;
            d[IX(i,j)] = s0*(t0*d0[IX(i0,j0)]+t1*d0[IX(i0,j1)])+ s1*(t0*d0[IX(i1,j0)]+t1*d0[IX(i1,j1)]);


        }
    }
    SetBnd(b,d);

  }
  
  void Diffuse(int b, float[] x, float[] x0, float diff){

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
  
  
  void AddDensity(int x, int y, float amount){
     int index = IX(x,y);
     this.dens[index] += amount;
  }
  
  void AddVelocity(int x, int y, float amountX, float amountY){
     int index = IX(x,y);
     this.v[index] += amountX;
     this.u[index] += amountY;
  }

  void Project(float[] u, float[] v, float[] p, float[] div){

    int i,j,k;
    float h;

    h = 1.0/N;
    for(i=1;i<=N;i++){
        for(j=1;j<=N;j++){
            div[IX(i,j)] = -0.5*h*(u[IX(i+1,j)]-u[IX(i-1,j)]+ v[IX(i,j+1)]-v[IX(i,j-1)]);
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
            u[IX(i,j)] -= 0.5*(p[IX(i+1,j)]-p[IX(i-1,j)])/h;
            v[IX(i,j)] -= 0.5*(p[IX(i,j+1)]-p[IX(i,j-1)])/h;
        }
    }
    SetBnd(1, u); SetBnd(2, v);

  }
  void DensStep (float[] x, float[] x0, float[] u, float[] v, float diff ){
    Diffuse ( 0, x0, x, diff);
    Advect ( 0, x, x0, u, v);
  }

  void VelStep(float[] u, float[] v, float[] u0, float[] v0, float visc, float dt){
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

  void RenderD(boolean dark, boolean code){
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
