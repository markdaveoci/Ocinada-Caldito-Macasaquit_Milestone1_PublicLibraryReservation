public final class Visitor {
    private final int visitorId;
    private final String name;
    private final String address;
    private final String purpose;

    private Visitor(VisitorBuilder builder){
        this.visitorId = builder.visitorId;
        this.name = builder.name;
        this.address = builder.address;
        this.purpose = builder.purpose;
    }

    public int getVisitorId(){
        return visitorId;
    }

    public String getName(){
        return name;
    }

    public String getAddress(){
        return address;
    }

    public String getPurpose(){
        return purpose;
    }

    public static class VisitorBuilder{

        private int visitorId;
        private String name;
        private String address;
        private String purpose;

        public VisitorBuilder setVisitorId(int visitorId){
            this.visitorId = visitorId;
            return this;
        }

        public VisitorBuilder setName(String name){
            this.name = name;
            return this;
        }

        public VisitorBuilder setAddress(String address){
            this.address = address;
            return this;
        }

        public VisitorBuilder setPurpose(String purpose){
            this.purpose = purpose;
            return this;
        }

        public Visitor build(){
            return new Visitor(this);
        }
    }
}