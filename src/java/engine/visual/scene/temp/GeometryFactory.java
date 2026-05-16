package engine.visual.scene.temp;

import engine.util.EngineConstraints;

import java.util.ArrayList;
import java.util.List;

final class GeometryFactory {

    private GeometryFactory() {}

    static float[] buildDemoGeometry() {
        List<Float> out = new ArrayList<>();
        addRoom(out, 14.5f, 14.5f);
        EngineConstraints.requireHermeticSpace(true, "Room shell is closed (floor, ceiling, 4 walls)");
        addCube(out, -3f, 1f, -1f, 2f, 0.9f, 0.3f, 0.2f);
        addCube(out, 2.7f, 1.4f, -2.2f, 2.8f, 0.2f, 0.8f, 0.4f);
        addSphere(out, 0f, 2.1f, 2.2f, 1.4f, 16, 10, 0.3f, 0.6f, 1f);
        addSphere(out, 4f, 0.9f, 2.6f, 0.8f, 12, 8, 0.95f, 0.9f, 0.2f);
        addQuadMarker(out);
        addTriangleMarker(out);
        float[] data = new float[out.size()];
        for (int i = 0; i < out.size(); i++) data[i] = out.get(i);
        return data;
    }

    private static void addRoom(List<Float> out, float halfSize, float height) {
        float x0 = -halfSize, x1 = halfSize;
        float z0 = -halfSize, z1 = halfSize;
        float y0 = 0f, y1 = height;
        float rf = 96f / 255f, gf = 0f, bf = 0f; // floor/ceiling red
        float rn = 0f, gn = 96f / 255f, bn = 0f; // north/south green
        float rw = 0f, gw = 0f, bw = 96f / 255f; // west/east blue

        tri(out, x0,y0,z0, x1,y0,z0, x1,y0,z1, rf,gf,bf, 0f,1f,0f);
        tri(out, x0,y0,z0, x1,y0,z1, x0,y0,z1, rf,gf,bf, 0f,1f,0f);
        tri(out, x0,y1,z0, x1,y1,z1, x1,y1,z0, rf,gf,bf, 0f,-1f,0f);
        tri(out, x0,y1,z0, x0,y1,z1, x1,y1,z1, rf,gf,bf, 0f,-1f,0f);

        tri(out, x1,y0,z0, x1,y1,z0, x1,y1,z1, rw,gw,bw, -1f,0f,0f);
        tri(out, x1,y0,z0, x1,y1,z1, x1,y0,z1, rw,gw,bw, -1f,0f,0f);
        tri(out, x0,y0,z0, x0,y1,z1, x0,y1,z0, rw,gw,bw, 1f,0f,0f);
        tri(out, x0,y0,z0, x0,y0,z1, x0,y1,z1, rw,gw,bw, 1f,0f,0f);

        tri(out, x0,y0,z1, x1,y1,z1, x0,y1,z1, rn,gn,bn, 0f,0f,-1f);
        tri(out, x0,y0,z1, x1,y0,z1, x1,y1,z1, rn,gn,bn, 0f,0f,-1f);
        tri(out, x0,y0,z0, x0,y1,z0, x1,y1,z0, rn,gn,bn, 0f,0f,1f);
        tri(out, x0,y0,z0, x1,y1,z0, x1,y0,z0, rn,gn,bn, 0f,0f,1f);
    }

    private static void addCube(List<Float> out, float cx, float cy, float cz, float s, float r, float g, float b) { float h=s*0.5f;
        tri(out,cx-h,cy-h,cz+h,cx+h,cy-h,cz+h,cx+h,cy+h,cz+h,r,g,b,0,0,1); tri(out,cx-h,cy-h,cz+h,cx+h,cy+h,cz+h,cx-h,cy+h,cz+h,r,g,b,0,0,1);
        tri(out,cx-h,cy-h,cz-h,cx+h,cy+h,cz-h,cx+h,cy-h,cz-h,r,g,b,0,0,-1); tri(out,cx-h,cy-h,cz-h,cx-h,cy+h,cz-h,cx+h,cy+h,cz-h,r,g,b,0,0,-1);
        tri(out,cx+h,cy-h,cz-h,cx+h,cy+h,cz-h,cx+h,cy+h,cz+h,r,g,b,1,0,0); tri(out,cx+h,cy-h,cz-h,cx+h,cy+h,cz+h,cx+h,cy-h,cz+h,r,g,b,1,0,0);
        tri(out,cx-h,cy-h,cz-h,cx-h,cy+h,cz+h,cx-h,cy+h,cz-h,r,g,b,-1,0,0); tri(out,cx-h,cy-h,cz-h,cx-h,cy-h,cz+h,cx-h,cy+h,cz+h,r,g,b,-1,0,0);
        tri(out,cx-h,cy+h,cz-h,cx-h,cy+h,cz+h,cx+h,cy+h,cz+h,r,g,b,0,1,0); tri(out,cx-h,cy+h,cz-h,cx+h,cy+h,cz+h,cx+h,cy+h,cz-h,r,g,b,0,1,0);
        tri(out,cx-h,cy-h,cz-h,cx+h,cy-h,cz+h,cx-h,cy-h,cz+h,r,g,b,0,-1,0); tri(out,cx-h,cy-h,cz-h,cx+h,cy-h,cz-h,cx+h,cy-h,cz+h,r,g,b,0,-1,0);
    }

    private static void addSphere(List<Float> out, float cx,float cy,float cz,float radius,int lon,int lat,float r,float g,float b){
        for(int y=0;y<lat;y++){
            float v0=(float)y/lat, v1=(float)(y+1)/lat;
            float t0=(float)Math.PI*v0, t1=(float)Math.PI*v1;
            for(int x=0;x<lon;x++){
                float u0=(float)x/lon,u1=(float)(x+1)/lon;
                float p0=(float)(u0*Math.PI*2),p1=(float)(u1*Math.PI*2);
                float[] a=sp(cx,cy,cz,radius,t0,p0), c=sp(cx,cy,cz,radius,t1,p0), d=sp(cx,cy,cz,radius,t1,p1), e=sp(cx,cy,cz,radius,t0,p1);
                triN(out,a,c,d,r,g,b); triN(out,a,d,e,r,g,b);
            }
        }
    }

    private static void addQuadMarker(List<Float> out){ tri(out,-6f,0.05f,4f,-4f,0.05f,4f,-4f,2f,4f,0.9f,0.2f,0.95f,0,0,1); tri(out,-6f,0.05f,4f,-4f,2f,4f,-6f,2f,4f,0.9f,0.2f,0.95f,0,0,1);}    
    private static void addTriangleMarker(List<Float> out){ tri(out,5f,0.1f,4.5f,6.5f,2.6f,4.2f,3.8f,2.4f,3.8f,0.1f,1f,0.8f,0,0,1);}    

    private static float[] sp(float cx,float cy,float cz,float r,float t,float p){ float sx=(float)(Math.sin(t)*Math.cos(p)); float sy=(float)Math.cos(t); float sz=(float)(Math.sin(t)*Math.sin(p)); return new float[]{cx+sx*r,cy+sy*r,cz+sz*r,sx,sy,sz}; }
    private static void triN(List<Float> o,float[] a,float[] b,float[] c,float r,float g,float bb){ v(o,a[0],a[1],a[2],r,g,bb,a[3],a[4],a[5]); v(o,b[0],b[1],b[2],r,g,bb,b[3],b[4],b[5]); v(o,c[0],c[1],c[2],r,g,bb,c[3],c[4],c[5]); }
    private static void tri(List<Float> o,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float r,float g,float bb,float nx,float ny,float nz){ v(o,ax,ay,az,r,g,bb,nx,ny,nz); v(o,bx,by,bz,r,g,bb,nx,ny,nz); v(o,cx,cy,cz,r,g,bb,nx,ny,nz);}    
    private static void v(List<Float> o,float x,float y,float z,float r,float g,float b,float nx,float ny,float nz){ o.add(x);o.add(y);o.add(z);o.add(r);o.add(g);o.add(b);o.add(nx);o.add(ny);o.add(nz);}    
}
