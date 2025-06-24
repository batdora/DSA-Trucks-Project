public class Truck {
        private final int truckID;
        private int currentLoad;
        private final int totalCapacity;
        Truck next; // Pointer to the next truck in the linked list

        // Create truck with linked list implementation
        public Truck(int truckID, int currentLoad, int totalCapacity) {
            this.truckID = truckID;
            this.currentLoad = currentLoad;
            this.totalCapacity = totalCapacity;
            this.next = null; // Initially, there's no next truck
        }

        public Truck(int truckID, int totalCapacity) {
             this(truckID,0,totalCapacity);
        }

        public int getRemainingLoad(){
            return totalCapacity-currentLoad;
        }


        // Getters for truck properties
        public int getTruckID() {
            return truckID;
        }

        public int getCurrentLoad() {
            return currentLoad;
        }

        public int getTotalCapacity() {
            return totalCapacity;
        }

        // Truck loader that can take in as argument more than its capacity and if so returns how much it loaded
        public int loadTruck(int loadAmount){
            // Returns how much is loaded
            if (loadAmount == getRemainingLoad()){
                currentLoad = 0;
            } else {
                currentLoad=loadAmount+currentLoad;
            }
            return loadAmount;
        }
    }
