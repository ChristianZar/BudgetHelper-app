package czb.budgethelper;

public class BudgetItem {

    private String name;
    private double price;

    public BudgetItem(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}