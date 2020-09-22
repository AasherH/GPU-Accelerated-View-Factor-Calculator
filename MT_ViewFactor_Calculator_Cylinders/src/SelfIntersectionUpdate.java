// Geometry partitioning class for geometries that require resolution of self-intersection


public class SelfIntersectionUpdate {

    private SelfIntersectionUpdate[] EmitBlock;
    ///////////////////////////////////////////////////////////////////
    //UPDATED EMITTER/RECEIVER
    private double[] normalXupdate;
    private double[] normalYupdate;
    private double[] normalZupdate;

    private double[] vertexAXupdate;
    private double[] vertexAYupdate;
    private double[] vertexAZupdate;

    private double[] edgeBAXupdate;
    private double[] edgeBAYupdate;
    private double[] edgeBAZupdate;

    private double[] edgeCAXupdate;
    private double[] edgeCAYupdate;
    private double[] edgeCAZupdate;

    private double[] centerXupdate;
    private double[] centerYupdate;
    private double[] centerZupdate;

    private double[] areaupdate;

    //UPDATED BLOCK
    private double[] normalXblock;
    private double[] normalYblock;
    private double[] normalZblock;

    private double[] vertexAXblock;
    private double[] vertexAYblock;
    private double[] vertexAZblock;

    private double[] edgeBAXblock;
    private double[] edgeBAYblock;
    private double[] edgeBAZblock;

    private double[] edgeCAXblock;
    private double[] edgeCAYblock;
    private double[] edgeCAZblock;

    private double[] centerXblock;
    private double[] centerYblock;
    private double[] centerZblock;

    private double[] areablock;

    //Original geometry values
    private double[] normalX;
    private double[] normalY;
    private double[] normalZ;

    private double[] vertexAX;
    private double[] vertexAY;
    private double[] vertexAZ;

    private double[] edgeBAX;
    private double[] edgeBAY;
    private double[] edgeBAZ;

    private double[] edgeCAX;
    private double[] edgeCAY;
    private double[] edgeCAZ;

    private double[] centerX;
    private double[] centerY;
    private double[] centerZ;

    private double[] area;
    private int size;


    //Produces the partitioned arrays for the self-intersecting geometries
    // The Geometry object that gets passed in will be updated
    public SelfIntersectionUpdate(Geometry geom){

        //Get the actual emitterFiles
        normalX = geom.getNormalX();
        normalY = geom.getNormalY();
        normalZ = geom.getNormalZ();

        vertexAX = geom.getVertexAX();
        vertexAY = geom.getVertexAY();
        vertexAZ = geom.getVertexAZ();

        edgeBAX = geom.getEdgeBAX();
        edgeBAY = geom.getEdgeBAY();
        edgeBAZ = geom.getEdgeBAZ();

        edgeCAX = geom.getEdgeCAX();
        edgeCAY = geom.getEdgeCAY();
        edgeCAZ = geom.getEdgeCAZ();

        centerX = geom.getCenterX();
        centerY = geom.getCenterY();
        centerZ = geom.getCenterZ();

        area = geom.getArea();
        size = geom.getSize();

        //Initialize the singleEmitter/singleReceiver variables
        normalXupdate = new double[1];
        normalYupdate = new double[1];
        normalZupdate = new double[1];

        vertexAXupdate = new double[1];
        vertexAYupdate = new double[1];
        vertexAZupdate = new double[1];

        edgeBAXupdate = new double[1];
        edgeBAYupdate = new double[1];
        edgeBAZupdate = new double[1];

        edgeCAXupdate = new double[1];
        edgeCAYupdate = new double[1];
        edgeCAZupdate = new double[1];

        centerXupdate = new double[1];
        centerYupdate = new double[1];
        centerZupdate = new double[1];

        areaupdate = new double[1];


        normalXblock = new double[size-1];
        normalYblock = new double[size-1];
        normalZblock = new double[size-1];

        vertexAXblock = new double[size-1];
        vertexAYblock = new double[size-1];
        vertexAZblock = new double[size-1];

        edgeBAXblock = new double[size-1];
        edgeBAYblock = new double[size-1];
        edgeBAZblock = new double[size-1];

        edgeCAXblock = new double[size-1];
        edgeCAYblock = new double[size-1];
        edgeCAZblock = new double[size-1];

        centerXblock = new double[size-1];
        centerYblock = new double[size-1];
        centerZblock = new double[size-1];

        areablock = new double[size-1];

    }

    public void update(int index) {

        // Assign the new tessellation to the correct index
        normalXupdate[0] = normalX[index];
        normalYupdate[0] = normalY[index];
        normalZupdate[0] = normalZ[index];

        vertexAXupdate[0] = vertexAX[index];
        vertexAYupdate[0] = vertexAY[index];
        vertexAZupdate[0] = vertexAZ[index];

        edgeBAXupdate[0] = edgeBAX[index];
        edgeBAYupdate[0] = edgeBAY[index];
        edgeBAZupdate[0] = edgeBAZ[index];

        edgeCAXupdate[0] = edgeCAX[index];
        edgeCAYupdate[0] = edgeCAY[index];
        edgeCAZupdate[0] = edgeCAZ[index];

        centerXupdate[0] = centerX[index];
        centerYupdate[0] = centerY[index];
        centerZupdate[0] = centerZ[index];

        areaupdate[0] = area[index];
    }

    public void updateBlock(int index){

        // Partition the geometry
        for (int i=0; i<size-1; i++){

            if (i < index){
                normalXblock[i] = normalX[i];
                normalYblock[i] = normalY[i];
                normalZblock[i] = normalZ[i];

                vertexAXblock[i] = vertexAX[i];
                vertexAYblock[i] = vertexAY[i];
                vertexAZblock[i] = vertexAZ[i];

                edgeBAXblock[i] = edgeBAX[i];
                edgeBAYblock[i] = edgeBAY[i];
                edgeBAZblock[i] = edgeBAZ[i];

                edgeCAXblock[i] = edgeCAX[i];
                edgeCAYblock[i] = edgeCAY[i];
                edgeCAZblock[i] = edgeCAZ[i];

                centerXblock[i] = centerX[i];
                centerYblock[i] = centerY[i];
                centerZblock[i] = centerZ[i];

                areablock[i] = area[i];
            }

            else if (i >= index){
                normalXblock[i] = normalX[i+1];
                normalYblock[i] = normalY[i+1];
                normalZblock[i] = normalZ[i+1];

                vertexAXblock[i] = vertexAX[i+1];
                vertexAYblock[i] = vertexAY[i+1];
                vertexAZblock[i] = vertexAZ[i+1];

                edgeBAXblock[i] = edgeBAX[i+1];
                edgeBAYblock[i] = edgeBAY[i+1];
                edgeBAZblock[i] = edgeBAZ[i+1];

                edgeCAXblock[i] = edgeCAX[i+1];
                edgeCAYblock[i] = edgeCAY[i+1];
                edgeCAZblock[i] = edgeCAZ[i+1];

                centerXblock[i] = centerX[i+1];
                centerYblock[i] = centerY[i+1];
                centerZblock[i] = centerZ[i+1];

                areablock[i] = area[i+1];
            }
        }
    }


    //EMITTER FUNCTIONS
    double [] getNormalX(){
        return normalXupdate;
    }

    double[] getNormalY() {
        return normalYupdate;
    }

    double[] getNormalZ() {
        return normalZupdate;
    }

    double[] getVertexAX() {
        return vertexAXupdate;
    }

    double[] getVertexAY() {
        return vertexAYupdate;
    }

    double[] getVertexAZ() {
        return vertexAZupdate;
    }

    double[] getEdgeBAX() {
        return edgeBAXupdate;
    }

    double[] getEdgeBAY() {
        return edgeBAYupdate;
    }

    double[] getEdgeBAZ() {
        return edgeBAZupdate;
    }

    double[] getEdgeCAX() {
        return edgeCAXupdate;
    }

    double[] getEdgeCAY() {
        return edgeCAYupdate;
    }

    double[] getEdgeCAZ() {
        return edgeCAZupdate;
    }

    double[] getCenterX() {
        return centerXupdate;
    }

    double[] getCenterY() {
        return centerYupdate;
    }

    double[] getCenterZ() {
        return centerZupdate;
    }

    int getSize(){
        return normalXupdate.length;
    }

    double[] getArea() {
        return areaupdate;
    }

    //BLOCKING FUNCTIONS

    double [] getNormalXblock(){
        return normalXblock;
    }

    double[] getNormalYblock() {
        return normalYblock;
    }

    double[] getNormalZblock() {
        return normalZblock;
    }

    double[] getVertexAXblock() {
        return vertexAXblock;
    }

    double[] getVertexAYblock() {
        return vertexAYblock;
    }

    double[] getVertexAZblock() {
        return vertexAZblock;
    }

    double[] getEdgeBAXblock() {
        return edgeBAXblock;
    }

    double[] getEdgeBAYblock() {
        return edgeBAYblock;
    }

    double[] getEdgeBAZblock() {
        return edgeBAZblock;
    }

    double[] getEdgeCAXblock() {
        return edgeCAXblock;
    }

    double[] getEdgeCAYblock() {
        return edgeCAYblock;
    }

    double[] getEdgeCAZblock() {
        return edgeCAZblock;
    }

    double[] getCenterXblock() {
        return centerXblock;
    }

    double[] getCenterYblock() {
        return centerYblock;
    }

    double[] getCenterZblock() {
        return centerZblock;
    }

    int getSizeblock(){
        return normalXblock.length;
    }

    double[] getAreablock() {
        return areablock;
    }
}
