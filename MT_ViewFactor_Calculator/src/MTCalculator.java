// Calculates the view factor via GPU-accelerated programming
// Ray casting and intersection detection occurs in this class
// Final view factor is returned to CalculateVF

import com.aparapi.Kernel;
import com.aparapi.Range;
import java.lang.Math;
import java.util.function.Consumer;

public class MTCalculator extends Kernel {

    // Instance variables
    private double[] result; // Contains the view factors for each specific job
    private double viewFactorResults[];
    private double viewFactor;

    private Geometry emitter;
    private Geometry receiver;
    private Geometry block;

    private final int emitterTessellations;
    private final double[] emitterNormalX;
    private final double[] emitterNormalY;
    private final double[] emitterNormalZ;
    private final double[] emitterVertexAX;
    private final double[] emitterVertexAY;
    private final double[] emitterVertexAZ;
    private final double[] emitterCenterX;
    private final double[] emitterCenterY;
    private final double[] emitterCenterZ;
    private final double[] emitterAreas;

    private final int blockTessellations;
    private final double[] blockNormalX;
    private final double[] blockNormalY;
    private final double[] blockNormalZ;
    private final double[] blockVertexAX;
    private final double[] blockVertexAY;
    private final double[] blockVertexAZ;
    private final double[] blockEdgeBAX;
    private final double[] blockEdgeBAY;
    private final double[] blockEdgeBAZ;
    private final double[] blockEdgeCAX;
    private final double[] blockEdgeCAY;
    private final double[] blockEdgeCAZ;

    private int receiverTessellations;
    private final double[] receiverNormalX;
    private final double[] receiverNormalY;
    private final double[] receiverNormalZ;
    private final double[] receiverVertexAX;
    private final double[] receiverVertexAY;
    private final double[] receiverVertexAZ;
    private final double[] receiverCenterX;
    private final double[] receiverCenterY;
    private final double[] receiverCenterZ;
    private final double[] receiverAreas;

    private int emitterIndex;
    private static final double PI = 3.141592653589793238462643383279502884197169399375105820974944592307816406286d;

    public interface KernelComplete {
        double onComplete();
    }


    public MTCalculator(Geometry emitter, Geometry receiver, Geometry block) {
        // Copy over constant final data
        this.emitter = emitter;
        this.receiver = receiver;
        this.block = block;

        emitterTessellations = emitter.getSize();
        emitterNormalX = emitter.getNormalX();
        emitterNormalY = emitter.getNormalY();
        emitterNormalZ = emitter.getNormalZ();
        emitterVertexAX = emitter.getVertexAX();
        emitterVertexAY = emitter.getVertexAY();
        emitterVertexAZ = emitter.getVertexAZ();
        emitterCenterX = emitter.getCenterX();
        emitterCenterY = emitter.getCenterY();
        emitterCenterZ = emitter.getCenterZ();
        emitterAreas = emitter.getArea();

        blockTessellations = block.getSize();
        blockNormalX = block.getNormalX();
        blockNormalY = block.getNormalY();
        blockNormalZ = block.getNormalZ();
        blockVertexAX = block.getVertexAX();
        blockVertexAY = block.getVertexAY();
        blockVertexAZ = block.getVertexAZ();
        blockEdgeBAX = block.getEdgeBAX();
        blockEdgeBAY = block.getEdgeBAY();
        blockEdgeBAZ = block.getEdgeBAZ();
        blockEdgeCAX = block.getEdgeCAX();
        blockEdgeCAY = block.getEdgeCAY();
        blockEdgeCAZ = block.getEdgeCAZ();

        receiverTessellations = receiver.getSize();
        receiverNormalX = receiver.getNormalX();
        receiverNormalY = receiver.getNormalY();
        receiverNormalZ = receiver.getNormalZ();
        receiverVertexAX = receiver.getVertexAX();
        receiverVertexAY = receiver.getVertexAY();
        receiverVertexAZ = receiver.getVertexAZ();
        receiverCenterX = receiver.getCenterX();
        receiverCenterY = receiver.getCenterY();
        receiverCenterZ = receiver.getCenterZ();
        receiverAreas = receiver.getArea();

        result = new double[receiverTessellations];
    }

    //Run on the GPU
    @Override
    public void run() { //gets called in CalculateVF when we call the execute method for the Kernel

        int receiverIndex = getGlobalId();

        // Calculate the ray from the emitter to the destination tessellation.
        double rayX = receiverCenterX[receiverIndex] - emitterCenterX[emitterIndex];
        double rayY = receiverCenterY[receiverIndex] - emitterCenterY[emitterIndex];
        double rayZ = receiverCenterZ[receiverIndex] - emitterCenterZ[emitterIndex];
        double rayMagnitude = vectorMagnitude(rayX, rayY, rayZ);

        // Check if any intersecting geometry exists. Iterate through every blocking tessellation
        for (int blockIndex = 0; blockIndex < blockTessellations; blockIndex++) {
            double intersectionDistance = intersectionDistance(blockIndex, rayX, rayY, rayZ);
            // If intersecting geometry exists, the contributed view factor is zero.
            if (intersectionDistance != 0 && intersectionDistance <= rayMagnitude) {
                result[receiverIndex] = 0;
                return;
            }
        }

        double emitterDenominator =
                vectorMagnitude(
                        emitterNormalX[emitterIndex],
                        emitterNormalY[emitterIndex],
                        emitterNormalZ[emitterIndex]) * rayMagnitude;
        double receiverDenominator =
                vectorMagnitude(
                        receiverNormalX[receiverIndex],
                        receiverNormalY[receiverIndex],
                        receiverNormalZ[receiverIndex]) * rayMagnitude;

        double emitterNormalDotRay =
                emitterNormalX[emitterIndex] * rayX
                        + emitterNormalY[emitterIndex] * rayY
                        + emitterNormalZ[emitterIndex] * rayZ;
        double receiverNormalDotRay =
                receiverNormalX[receiverIndex] * rayX
                        + receiverNormalY[receiverIndex] * rayY
                        + receiverNormalZ[receiverIndex] * rayZ;

        double cosThetaOne = emitterNormalDotRay / emitterDenominator;
        double cosThetaTwo = receiverNormalDotRay / receiverDenominator;

        if (cosThetaOne < 0) {
            cosThetaOne = -cosThetaOne;
        }
        if (cosThetaTwo < 0){
            cosThetaTwo = -cosThetaTwo;
        }


        result[receiverIndex] = cosThetaOne * cosThetaTwo * emitterAreas[emitterIndex] * receiverAreas[receiverIndex]
                / (PI * rayMagnitude * rayMagnitude);

        // Uncomment if interested in the view factor for a ray cast
        //System.out.println(result[receiverIndex]);


    }

    public double calculate(Consumer<double[]> resultConsumer, KernelComplete completionHandler){
        viewFactorResults = new double[emitterTessellations];

        // Explicitly pass only these values onto the GPU
        setExplicit(true);
        put(emitterNormalX).put(emitterNormalY).put(emitterNormalZ);
        put(emitterCenterX).put(emitterCenterY).put(emitterCenterZ);
        put(emitterAreas);

        put(blockVertexAX).put(blockVertexAY).put(blockVertexAZ);
        put(blockEdgeBAX).put(blockEdgeBAY).put(blockEdgeBAZ);
        put(blockEdgeCAX).put(blockEdgeCAY).put(blockEdgeCAZ);

        put(receiverNormalX).put(receiverNormalY).put(receiverNormalZ);
        put(receiverCenterX).put(receiverCenterY).put(receiverCenterZ);
        put(receiverAreas);

        for(emitterIndex = 0; emitterIndex < emitterTessellations; emitterIndex++){
            super.execute(Range.create(receiverTessellations));
            get(result);
            resultConsumer.accept(result);
        }
        viewFactor = completionHandler.onComplete()/sum(emitterAreas);
        return viewFactor;
    }

    private double intersectionDistance(int interconnectIndex, double rayX, double rayY, double rayZ) {
        // MT Algorithm for intersection detection

        double pvecX = rayY * blockEdgeCAZ[interconnectIndex] - rayZ * blockEdgeCAY[interconnectIndex];
        double pvecY = rayZ * blockEdgeCAX[interconnectIndex] - rayX * blockEdgeCAZ[interconnectIndex]; //negative is accounted for by switching order of ops
        double pvecZ = rayX * blockEdgeCAY[interconnectIndex] - rayY * blockEdgeCAX[interconnectIndex];

        double intersectionDistance = -1;

        // Dot product of edge1 and pvec.
        double det = blockEdgeBAX[interconnectIndex] * pvecX
                + blockEdgeBAY[interconnectIndex] * pvecY
                + blockEdgeBAZ[interconnectIndex] * pvecZ;

        //Back-face culling enabled
        if (det < 0){
            return 0;
        }

        // Ray is parallel to plane. Thus, no intersection since dot product of orthogonal vectors is 0
        if (det < 1e-8 && det > -1e-8){
            return 0;
        }

        double invDet = 1 / det;

        double tvecX = emitterCenterX[emitterIndex] - blockVertexAX[interconnectIndex];
        double tvecY = emitterCenterY[emitterIndex] - blockVertexAY[interconnectIndex];
        double tvecZ = emitterCenterZ[emitterIndex] - blockVertexAZ[interconnectIndex];

        double u = (tvecX * pvecX + tvecY * pvecY + tvecZ * pvecZ) * invDet; //normalize u

        if (u < 0 || u > 1){
            return 0;
        }

        double qvecX = tvecY * blockEdgeBAZ[interconnectIndex] - tvecZ * blockEdgeBAY[interconnectIndex];
        double qvecY = tvecZ * blockEdgeBAX[interconnectIndex] - tvecX * blockEdgeBAZ[interconnectIndex];
        double qvecZ = tvecX * blockEdgeBAY[interconnectIndex] - tvecY * blockEdgeBAX[interconnectIndex];

        double v = (rayX * qvecX + rayY * qvecY + rayZ * qvecZ) * invDet; //normalize v

        if (v < 0 || (u + v) > 1 ) {
            return 0;
        }
        else {
            return (blockEdgeCAX[interconnectIndex] * qvecX
                    + blockEdgeCAY[interconnectIndex] * qvecY
                    + blockEdgeCAZ[interconnectIndex] * qvecZ) * invDet;

        }
    }


    public double vectorMagnitude(double x, double y, double z) {
        return Math.sqrt(x*x + y*y + z*z);
    }

    private static double sum(double...values) {
        double result = 0;
        for (int index=0;index<values.length; index++){
            result = result + values[index];
        }
        return result;
    }
}