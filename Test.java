public class HelloWorld {

    public void sayHello() {
        aggregate((empty protoFile, empty item) -> {
            return request.getFileToGenerateList();
        }).sum();
    }
}
