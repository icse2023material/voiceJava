public class Puppy {

    private int puppyAge;

    private int puppyNum;

    public void setAge(int age) {
        puppyAge = age;
    }

    public int getAge() {
        return puppyAge;
    }

    public static void main(String[] args) {
        Puppy myPuppy = new Puppy("tommy");
        myPuppy.setAge(2);
        myPuppy.getAge();
        System.out.println(myPuppy.puppyAge);
    }
}
