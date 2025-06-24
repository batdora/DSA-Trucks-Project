public class LinkedList {
    // Head of the linked list (first truck in the queue)
    private Truck head;
    private int numberOfTrucks=0;

    public int getNumberOfTrucks() {
        return numberOfTrucks;
    }

    public Truck getHead() {
        return head;
    }

    // Add a truck to the end of the list
    public void addTruck(Truck truck) {
        // If the list is empty, set new truck as head
        if (head == null) {
            head = truck;
        } else {
            // Traverse to the last truck
            Truck current = head;
            while (current.next != null) {
                current = current.next;
            }
            // Link the new truck at the end
            current.next = truck;
        }
        // Ensure the added truck has no next truck yet
        truck.next = null;
        numberOfTrucks ++;
    }

    public Truck removeHeadTruck(){
        return removeTruckByID(this.head.getTruckID());
    }

    // Remove and return a specific truck by its ID
    public Truck removeTruckByID(int truckID) {
        // Return null if the list is empty
        if (head == null) {
            return null;
        }
        // Disconnect the removed truck if it is the head
        if (head.getTruckID() == truckID) {
            Truck removedTruck = head;
            head = head.next; // Make the 2nd truck the head
            removedTruck.next = null; // Break the link of the removed truck with the others
            numberOfTrucks --;
            return removedTruck;
        }
        // Traverse to find the truck with the given ID
        Truck current = head;
        /// Only Step That Makes Time Complexity O(n) = O(lambda)
        while (current.next != null && current.next.getTruckID() != truckID) {
            current = current.next;
        }
        // Remove and break the links of the selected truck
        if (current.next != null) {
            Truck removedTruck = current.next;
            current.next = current.next.next; // Skip over the removed truck
            removedTruck.next = null; // Disconnect the removed truck
            numberOfTrucks --;
            return removedTruck;
        }

        return null; // Truck not found
    }
}


