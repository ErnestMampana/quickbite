package za.co.ernest.quickbite.resturantService.enums;

public enum ProductCategory {

    KOTA {
        @Override
        public void validateConstraints(CreateMenuItemRequest request) {
            // Kotas must mention standard fillings in the description for QA
            if (request.description() == null || !request.description().toLowerCase().contains("chips")) {
                throw new IllegalArgumentException("A authentic Kota description must specify baseline ingredients (e.g., chips).");
            }
        }
    },
    MEAL {
        @Override
        public void validateConstraints(CreateMenuItemRequest request) {
            // Base validation for standard cooked meals
            if (request.name().length() < 3) {
                throw new IllegalArgumentException("Meal titles must be descriptive.");
            }
        }
    },
    DRINK {
        @Override
        public void validateConstraints(CreateMenuItemRequest request) {
            // Drinks might have a fixed ceiling price constraint in your app
        }
    };

    // Every new category added forces the developer to implement its own structural rule
    public abstract void validateConstraints(CreateMenuItemRequest request);
}
