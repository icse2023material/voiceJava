public class HelloWorld {

    public void sayHello(Name[] names) {
        for (int i = 0; i < names.length; i++) {
            if (i > 90) {
                continue;
            } else if (i == 90) {
                continue;
            } else {
                notify();
                continue;
            }
        }
    }
}
