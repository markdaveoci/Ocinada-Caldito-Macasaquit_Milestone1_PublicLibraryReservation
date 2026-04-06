public class ReferenceMaterial {

    private final int materialId;
    private final String referenceMaterial;
    private final String status;

    public ReferenceMaterial(int materialId, String referenceMaterial, String status) {
        this.materialId = materialId;
        this.referenceMaterial = referenceMaterial;
        this.status = status;
    }

    public int getMaterialId() {
        return materialId;
    }

    public String getReferenceMaterial() {
        return referenceMaterial;
    }

    public String getStatus() {
        return status;
    }
}

