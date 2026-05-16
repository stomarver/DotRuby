package engine.util.shader;

import org.joml.Vector3f;

import java.util.*;

public final class StencilShadows {
    private final float extrudeDistance;

    public StencilShadows(float extrudeDistance) { this.extrudeDistance = extrudeDistance; }

    public float[] buildVolume(float[] sceneVertices, Vector3f lightPos) {
        int triCount = sceneVertices.length / 27;
        boolean[] lit = new boolean[triCount];

        for (int t = 0; t < triCount; t++) {
            int b = t * 27;
            Vector3f a = new Vector3f(sceneVertices[b], sceneVertices[b+1], sceneVertices[b+2]);
            Vector3f c = new Vector3f(sceneVertices[b+9], sceneVertices[b+10], sceneVertices[b+11]);
            Vector3f d = new Vector3f(sceneVertices[b+18], sceneVertices[b+19], sceneVertices[b+20]);
            Vector3f n = new Vector3f(c).sub(a).cross(new Vector3f(d).sub(a)).normalize();
            Vector3f ctr = new Vector3f(a).add(c).add(d).mul(1f/3f);
            lit[t] = n.dot(new Vector3f(lightPos).sub(ctr).normalize()) > 0f;
        }

        Map<String, int[]> edgeToTri = new HashMap<>();
        List<Vector3f> verts = new ArrayList<>();
        for (int t = 0; t < triCount; t++) {
            int b = t*27;
            Vector3f[] v = {new Vector3f(sceneVertices[b],sceneVertices[b+1],sceneVertices[b+2]), new Vector3f(sceneVertices[b+9],sceneVertices[b+10],sceneVertices[b+11]), new Vector3f(sceneVertices[b+18],sceneVertices[b+19],sceneVertices[b+20])};
            for (int e=0;e<3;e++) {
                String k=edgeKey(v[e],v[(e+1)%3]);
                int[] pair=edgeToTri.get(k);
                if(pair==null) edgeToTri.put(k,new int[]{t,-1}); else pair[1]=t;
            }
        }
        for (var en: edgeToTri.entrySet()) {
            int t0=en.getValue()[0], t1=en.getValue()[1];
            if (!(t1==-1 || lit[t0] != lit[t1])) continue;
            String[] p=en.getKey().split("\\|");
            Vector3f v0=parseV(p[0]), v1=parseV(p[1]);
            Vector3f v0e=extrude(v0,lightPos), v1e=extrude(v1,lightPos);
            addTri(verts,v0,v1,v0e); addTri(verts,v1,v1e,v0e);
        }
        for (int t=0;t<triCount;t++) {
            int b=t*27;
            Vector3f a=new Vector3f(sceneVertices[b],sceneVertices[b+1],sceneVertices[b+2]);
            Vector3f c=new Vector3f(sceneVertices[b+9],sceneVertices[b+10],sceneVertices[b+11]);
            Vector3f d=new Vector3f(sceneVertices[b+18],sceneVertices[b+19],sceneVertices[b+20]);
            if (lit[t]) addTri(verts,a,c,d);
            else addTri(verts,extrude(a,lightPos),extrude(d,lightPos),extrude(c,lightPos));
        }
        float[] arr = new float[verts.size()*3];
        for(int i=0;i<verts.size();i++){arr[i*3]=verts.get(i).x;arr[i*3+1]=verts.get(i).y;arr[i*3+2]=verts.get(i).z;}
        return arr;
    }

    private String edgeKey(Vector3f a, Vector3f b){String sa=fmt(a), sb=fmt(b); return sa.compareTo(sb)<=0?sa+"|"+sb:sb+"|"+sa;}
    private String fmt(Vector3f v){return String.format(java.util.Locale.US,"%.4f,%.4f,%.4f",v.x,v.y,v.z);}    
    private Vector3f parseV(String s){String[] p=s.split(",");return new Vector3f(Float.parseFloat(p[0]),Float.parseFloat(p[1]),Float.parseFloat(p[2]));}
    private Vector3f extrude(Vector3f v, Vector3f l){return new Vector3f(v).add(new Vector3f(v).sub(l).normalize().mul(extrudeDistance));}
    private void addTri(List<Vector3f> out, Vector3f a, Vector3f b, Vector3f c){out.add(a);out.add(b);out.add(c);}    
}
