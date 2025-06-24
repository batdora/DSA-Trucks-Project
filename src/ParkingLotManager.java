public class ParkingLotManager {

    // Initialize the AVL trees for managing parking lots and ready lots
    public Ledger ledger;
    public ReadyLedger readyLedger;
    public WaitingLedger waitingLedger;

    public ParkingLotManager() {
        this.ledger = new Ledger();
        this.readyLedger = new ReadyLedger();
        this.waitingLedger = new WaitingLedger();
    }

    public class ParkingLot {
        private int load_capacity;
        private int truck_capacity;
        private LinkedList waitingSection;
        private LinkedList readySection;
        private boolean isInReadyLedger = false; // Indicates if the lot is in readyLedger
        public boolean isInWaitingLedger = false; // Indicates if the lot is in waitingLedger

        // Parking Lot Constructor
        public ParkingLot(int load_capacity, int truck_capacity) {
            this.load_capacity = load_capacity;
            this.truck_capacity = truck_capacity;
            this.waitingSection = new LinkedList();
            this.readySection = new LinkedList();
        }

        public int getTruckNum() {
            return waitingSection.getNumberOfTrucks() + readySection.getNumberOfTrucks();
        }

        public int capacityStatus() {
            return truck_capacity - getTruckNum();
        }

        // Overloaded constructor with default truck capacity
        public ParkingLot(int load_capacity) {
            this(load_capacity, 100);
        }

        // Method to add a parking lot by creating a new ParkingLot and inserting into Ledger
        public void addParkingLot(int loadCapacity, int truckCapacity) {
            ParkingLot newLot = new ParkingLot(loadCapacity, truckCapacity);
            ledger.insert(newLot); // Insert into the Ledger
        }

        // Overloaded method to add a parking lot with default truck capacity
        public void addParkingLot(int loadCapacity) {
            ParkingLot newLot = new ParkingLot(loadCapacity);
            ledger.insert(newLot); // Insert into the Ledger
        }

        // Truck adder to the parking lot, all trucks must enter from the waiting section. Also checks if the lot is full.
        public int addTruckToWaiting(Truck truck) {
            int result;
            if (getTruckNum() < truck_capacity) {
                waitingSection.addTruck(truck);
                System.out.println("Truck successfully added to waiting section of the parking lot with " + load_capacity + " load capacity");

                // Insert this parking lot into waitingLedger if not already there
                if (!isInWaitingLedger) {
                    ParkingLotManager.this.waitingLedger.insert(new WaitingNode(this));
                    isInWaitingLedger = true;
                }

                // After adding the truck, update totalTrucks in the WaitingLedger
                WaitingNode node = waitingLedger.findNode(this.load_capacity);
                if (node != null) {
                    updateTotalTrucksUpwards(node);
                }

                return load_capacity;
            } else {
                System.out.println("Unfortunately the lot with " + load_capacity + " load capacity is full. We will be relocating the truck");
                if (load_capacity == 1) {
                    return -1;
                }
                result = ledger.assigner(truck, load_capacity);
                if (result == -1) {
                    System.out.println("Unable to relocate the truck. No suitable parking lot found.");
                    return -2;
                }
                return result;
            }


        }

        private void updateTotalTrucksUpwards(WaitingNode node) {
            while (node != null) {
                node.totalTrucks = getTotalTrucks(node.left) + getTotalTrucks(node.right) + node.lot.waitingSection.getNumberOfTrucks();
                node = node.parent;
            }
        }

        private void updateTotalTrucksUpwards(ReadyNode node) {
            while (node != null) {
                node.totalTrucks = getTotalTrucks(node.left) + getTotalTrucks(node.right) + node.lot.readySection.getNumberOfTrucks();
                node = node.parent;
            }
        }


        // Ready Command
        public int[] ready() {

            // Remove truck from waiting section
            Truck truck = waitingSection.removeHeadTruck();

            // Update totalTrucks in WaitingLedger
            WaitingNode waitingNode = ParkingLotManager.this.waitingLedger.findNode(this.load_capacity);
            if (waitingNode != null) {
                updateTotalTrucksUpwards(waitingNode);
            }

            readySection.addTruck(truck); // Add to ready section

            // Remove this parking lot from waitingLedger if waitingSection is now empty
            if (waitingSection.getNumberOfTrucks() == 0 && isInWaitingLedger) {
                ParkingLotManager.this.waitingLedger.delete(getLoadCapacity());
                isInWaitingLedger = false;
            }

            // Insert this parking lot into readyLedger if not already there
            if (!isInReadyLedger) {
                ParkingLotManager.this.readyLedger.insert(new ReadyNode(this));
                isInReadyLedger = true;
            }

            // Update totalTrucks in ReadyLedger
            ReadyNode readyNode = ParkingLotManager.this.readyLedger.findNode(this.load_capacity);
            if (readyNode != null) {
                updateTotalTrucksUpwards(readyNode);
            }

            System.out.println("READY. The truck with ID " + truck.getTruckID() + " is ready in lot " + load_capacity);

            return new int[]{truck.getTruckID(), load_capacity};
        }

        public int getLoadCapacity() {
            return load_capacity;
        }
    }

    public String load(ReadyNode load_node, int amount) {

        if ((load_node == null) || (load_node.lot.load_capacity == 0)) {
            System.out.println("No parking lot with capacity found to load.");
            return "-1";
        }

        StringBuilder result1 = new StringBuilder();
        StringBuilder result2 = new StringBuilder();
        StringBuilder result3 = new StringBuilder();

        ParkingLot load_lot = load_node.lot;

        // Loop to find a lot with available trucks
        while (load_node != null && load_lot.readySection.getNumberOfTrucks() == 0) {
            int temp_capacity = load_lot.load_capacity;
            load_node = readyLedger.findClosestGreater(temp_capacity);
            if (load_node != null) {
                load_lot = load_node.lot;
            }
        }

        if (load_node == null) {
            System.out.println("No parking lot with capacity found to load.");
            return "-1";
        }

        int ready = load_lot.readySection.getNumberOfTrucks();
        int current_amount = amount;
        int truckLoadResult; // How much the truck loaded
        Truck local_truck; // The iterated truck
        int load_capacity = load_lot.load_capacity;
        ParkingLot newLot;

        int iteration_amount; // How many trucks will be filled with full load amount
        int remaining_amount; // The amount of load that is not full load amount

        int result_capacity;

        if ((ready * load_capacity) < amount) {
            iteration_amount = ready;
            remaining_amount = 0;
        } else {
            iteration_amount = amount / load_capacity;
            remaining_amount = amount % load_capacity;
        }

        // If at least one truck can be loaded with the total capacity the lot can deliver
        if (iteration_amount >= 1) {
            for (int i = 0; i < iteration_amount; i++) {
                // If there are more ready trucks than amount break
                if (current_amount == 0) {
                    break;
                }

                // Fill trucks with load capacity
                truckLoadResult = load_lot.readySection.getHead().loadTruck(load_lot.load_capacity);
                current_amount -= truckLoadResult;

                // Relocate loaded or emptied truck to a new lot
                local_truck = load_lot.readySection.removeHeadTruck();

                // Check if readySection is empty after removal
                if (load_lot.readySection.getNumberOfTrucks() == 0 && load_lot.isInReadyLedger) {
                    readyLedger.delete(load_lot.getLoadCapacity());
                    load_lot.isInReadyLedger = false;
                }

                // Update totalTrucks in ReadyLedger
                ReadyNode readyNode = readyLedger.findNode(load_lot.getLoadCapacity());
                if (readyNode != null) {
                    load_lot.updateTotalTrucksUpwards(readyNode);
                }

                newLot = ledger.findLot(local_truck.getRemainingLoad());

                // Loop to find a suitable lot
                while (newLot != null) {
                    if (newLot.capacityStatus() != 0) {
                        // The lot has capacity; proceed to add the truck
                        break;
                    } else {
                        // The lot is full; find the next suitable lot
                        int temp_capacity = newLot.getLoadCapacity();
                        newLot = ledger.findClosestSmaller(temp_capacity);
                    }
                }

                // Check if a suitable lot was found
                if (newLot == null) {
                    System.out.println("After loading the truck, the truck with ID " + local_truck.getTruckID() + " could not be placed.");
                    result_capacity = -1;
                } else {
                    newLot.addTruckToWaiting(local_truck);
                    result_capacity = newLot.load_capacity;
                    System.out.println("Truck with ID " + local_truck.getTruckID() + " has been placed to lot with capacity " + newLot.getLoadCapacity());
                }

                result1.append(local_truck.getTruckID()).append(" ").append(result_capacity).append(" - ");
            }
        }

        // If there is remaining load
        if (remaining_amount != 0) {
            // Fill trucks with remaining capacity
            truckLoadResult = load_lot.readySection.getHead().loadTruck(remaining_amount);
            current_amount = 0;

            // Relocate loaded or emptied truck to a new lot
            local_truck = load_lot.readySection.removeHeadTruck();

            // Check if readySection is empty after removal
            if (load_lot.readySection.getNumberOfTrucks() == 0 && load_lot.isInReadyLedger) {
                readyLedger.delete(load_lot.getLoadCapacity());
                load_lot.isInReadyLedger = false;
            }

            // Update totalTrucks in ReadyLedger
            ReadyNode readyNode = readyLedger.findNode(load_lot.getLoadCapacity());
            if (readyNode != null) {
                load_lot.updateTotalTrucksUpwards(readyNode);
            }

            newLot = ledger.findLot(local_truck.getRemainingLoad());

            // Loop to find a suitable lot
            while (newLot != null) {
                if (newLot.capacityStatus() != 0) {
                    // Found lot with capacity
                    break;
                } else {
                    // The lot is full; find the next suitable lot
                    int temp_capacity = newLot.getLoadCapacity();
                    newLot = ledger.findClosestSmaller(temp_capacity);
                }
            }

            // Check if a suitable lot was found
            if (newLot == null) {
                System.out.println("After loading the truck, the truck with ID " + local_truck.getTruckID() + " could not be placed.");
                result_capacity = -1;
            } else {
                newLot.addTruckToWaiting(local_truck);
                result_capacity = newLot.load_capacity;
                System.out.println("Truck with ID " + local_truck.getTruckID() + " has been placed to lot with capacity " + newLot.getLoadCapacity());
            }

            result2.append(local_truck.getTruckID()).append(" ").append(result_capacity).append(" - ");
        }

        // If there is still load but the current lot cannot fulfill it
        while (current_amount != 0) {
            load_node = readyLedger.findClosestGreater(load_capacity);

            if (load_node != null) {
                load_lot = load_node.lot;
            } else {
                break;
            }

            // Loop to find a lot with available trucks
            while (load_node != null && load_lot.readySection.getNumberOfTrucks() == 0) {
                int temp_capacity = load_lot.load_capacity;
                load_node = readyLedger.findClosestGreater(temp_capacity);
                if (load_node != null) {
                    load_lot = load_node.lot;
                }
            }

            if (load_node != null) {
                // Found a lot with available trucks
                int loadCapacity = load_lot.load_capacity;
                int loadAmount = Math.min(current_amount, loadCapacity);

                // Load the truck
                Truck truck = load_lot.readySection.getHead();
                truckLoadResult = truck.loadTruck(loadAmount);
                current_amount -= truckLoadResult;

                // Relocate the truck
                local_truck = load_lot.readySection.removeHeadTruck();

                // Check if readySection is empty after removal
                if (load_lot.readySection.getNumberOfTrucks() == 0 && load_lot.isInReadyLedger) {
                    readyLedger.delete(load_lot.getLoadCapacity());
                    load_lot.isInReadyLedger = false;
                }

                // Update totalTrucks in ReadyLedger
                ReadyNode readyNode = readyLedger.findNode(load_lot.getLoadCapacity());
                if (readyNode != null) {
                    load_lot.updateTotalTrucksUpwards(readyNode);
                }

                ParkingLot destinationLot = ledger.findLot(local_truck.getRemainingLoad());

                // Find a suitable lot for the relocated truck
                while (destinationLot != null && destinationLot.capacityStatus() == 0) {
                    int tempCapacity = destinationLot.getLoadCapacity();
                    destinationLot = ledger.findClosestSmaller(tempCapacity);
                }

                if (destinationLot == null) {
                    System.out.println("After loading the truck, the truck with ID " + local_truck.getTruckID() + " could not be placed.");
                    result_capacity = -1;
                } else {
                    destinationLot.addTruckToWaiting(local_truck);
                    result_capacity = destinationLot.load_capacity;
                    System.out.println("Truck with ID " + local_truck.getTruckID() + " has been placed to lot with capacity " + destinationLot.getLoadCapacity());
                }

                // Accumulate the result
                result3.append(local_truck.getTruckID()).append(" ").append(result_capacity).append(" - ");
            } else {
                // No suitable parking lot found
                System.out.println("No parking lot with capacity found.");
                break;
            }
        }

        // Combine all results
        String finalResult = result1.toString() + result2.toString() + result3.toString();

        // Remove trailing " - " if present
        if (finalResult.endsWith(" - ")) {
            finalResult = finalResult.substring(0, finalResult.length() - 3);
        }

        // If no load was processed, return "-1"
        if (finalResult.isEmpty()) {
            return "-1";
        }

        return finalResult;
    }

    public int countTrucks(int capacity) {
        int waitingCount = countWaitingTrucks(waitingLedger.root, capacity);
        int readyCount = countReadyTrucks(readyLedger.root, capacity);
        int total = waitingCount + readyCount;
        System.out.println("Total trucks with lot capacity greater than " + capacity + " is: " + total);
        return total;
    }

    private int countWaitingTrucks(WaitingNode node, int capacity) {
        if (node == null) {
            return 0;
        }
        if (node.lot.getLoadCapacity() > capacity) {
            // Include totalTrucks from the right subtree
            int count = getTotalTrucks(node.right);

            // Include trucks from this parking lot's waiting section
            count += node.lot.waitingSection.getNumberOfTrucks();

            // Traverse the left subtree
            count += countWaitingTrucks(node.left, capacity);

            return count;
        } else {
            // Skip left subtree, traverse right subtree
            return countWaitingTrucks(node.right, capacity);
        }
    }

    private int countReadyTrucks(ReadyNode node, int capacity) {
        if (node == null) {
            return 0;
        }
        if (node.lot.getLoadCapacity() > capacity) {
            // Include totalTrucks from the right subtree
            int count = getTotalTrucks(node.right);

            // Include trucks from this parking lot's ready section
            count += node.lot.readySection.getNumberOfTrucks();

            // Traverse the left subtree
            count += countReadyTrucks(node.left, capacity);

            return count;
        } else {
            // Skip left subtree, traverse right subtree
            return countReadyTrucks(node.right, capacity);
        }
    }

    public int truckAdder(int truckID, int loadCapacity) {
        Truck truck = new Truck(truckID, loadCapacity);
        return truckAdder(truck);
    }

    public int truckAdder(Truck truck) {
        ParkingLot lot = ledger.findLot(truck.getTotalCapacity());
        if (lot == null) {
            return -1;
        } else {
            return lot.addTruckToWaiting(truck);
        }
    }

    // AVL tree structure for parking lots
    public class Ledger {
        private LedgerNode root;

        public ParkingLot findLot(int loadCapacity) {
            LedgerNode current = root;
            LedgerNode min = minValueNode(root);

            if (min != null && min.lot.load_capacity == loadCapacity) {
                return min.lot;
            }

            while (current != null) {
                if (loadCapacity < current.getLoadCapacity()) {
                    current = current.left;
                } else if (loadCapacity > current.getLoadCapacity()) {
                    current = current.right;
                } else {
                    return current.lot;
                }
            }

            LedgerNode closestSmallerNode = findClosestSmallerNode(loadCapacity);
            return closestSmallerNode != null ? closestSmallerNode.lot : null;
        }

        public LedgerNode findClosestSmallerNode(int capacity) {
            LedgerNode current = root;
            LedgerNode closestSmaller = null;

            if (capacity == 1) {
                return null;
            }

            while (current != null) {
                if (current.getLoadCapacity() < capacity) {
                    closestSmaller = current;
                    current = current.right;
                } else {
                    current = current.left;
                }
            }
            return closestSmaller;
        }

        public ParkingLot findClosestSmaller(int capacity) {
            LedgerNode node = findClosestSmallerNode(capacity);
            return node != null ? node.lot : null;
        }

        // Function to find and assign trucks to lots with the closest smaller capacity for lots that are full
        public int assigner(Truck truck, int capacity) {
            ParkingLot nextLot = findClosestSmaller(capacity);
            if (nextLot == null) {
                System.out.println("No smaller parking lot available to assign the truck.");
                return -2;
            } else {
                int response = -1;
                ParkingLot prevLot = null;

                // Add truck to the found lot
                response = nextLot.addTruckToWaiting(truck);

                // Iterate because the found lot can also be full
                while (response == -1) {
                    if (nextLot == null) {
                        return -1;
                    }

                    if (nextLot.load_capacity == 1) {
                        return -1;
                    }

                    response = nextLot.addTruckToWaiting(truck);

                    if ((response == -1) || (nextLot.load_capacity != 1)) {
                        prevLot = nextLot;
                        nextLot = findClosestSmaller(prevLot.getLoadCapacity());
                    }
                }
                return response;
            }
        }

        // Helper function to get the height of a node
        private int height(LedgerNode node) {
            return node == null ? 0 : node.height;
        }

        // Right rotation for balancing
        private LedgerNode rightRotate(LedgerNode y) {
            LedgerNode x = y.left;
            LedgerNode T2 = x.right;

            // Perform rotation
            x.right = y;
            y.left = T2;

            // Update parents
            if (T2 != null) {
                T2.parent = y;
            }
            x.parent = y.parent;
            y.parent = x;

            // Update the child pointer in the parent
            if (x.parent != null) {
                if (x.parent.left == y) {
                    x.parent.left = x;
                } else if (x.parent.right == y) {
                    x.parent.right = x;
                }
            } else {
                root = x; // x becomes the new root
            }

            // Update heights
            y.height = Math.max(height(y.left), height(y.right)) + 1;
            x.height = Math.max(height(x.left), height(x.right)) + 1;

            return x; // New root after rotation
        }

        // Left rotation for balancing
        private LedgerNode leftRotate(LedgerNode x) {
            LedgerNode y = x.right;
            LedgerNode T2 = y.left;

            // Perform rotation
            y.left = x;
            x.right = T2;

            // Update parents
            if (T2 != null) {
                T2.parent = x;
            }
            y.parent = x.parent;
            x.parent = y;

            // Update the child pointer in the parent
            if (y.parent != null) {
                if (y.parent.left == x) {
                    y.parent.left = y;
                } else if (y.parent.right == x) {
                    y.parent.right = y;
                }
            } else {
                root = y; // y becomes the new root
            }

            // Update heights
            x.height = Math.max(height(x.left), height(x.right)) + 1;
            y.height = Math.max(height(y.left), height(y.right)) + 1;

            return y; // New root after rotation
        }

        // Get balance factor of a node
        private int getBalance(LedgerNode node) {
            return node == null ? 0 : height(node.left) - height(node.right);
        }

        // Method to insert a parking lot to the Ledger
        public void insert(ParkingLot newLot) {
            LedgerNode newNode = new LedgerNode(newLot);
            root = insert(root, newNode);
            if (root != null) {
                root.parent = null;
            }
        }

        // Internal recursive insert function with balancing
        private LedgerNode insert(LedgerNode node, LedgerNode newNode) {
            if (node == null) {
                System.out.println("Lot with capacity " + newNode.getLoadCapacity() + " inserted");
                return newNode; // Insert new node at the correct position
            }

            // Recursion that traverses based on load capacity
            if (newNode.getLoadCapacity() < node.getLoadCapacity()) {
                node.left = insert(node.left, newNode);
                if (node.left != null)
                    node.left.parent = node;
            } else if (newNode.getLoadCapacity() > node.getLoadCapacity()) {
                node.right = insert(node.right, newNode);
                if (node.right != null)
                    node.right.parent = node;
            } else {
                return node; // Duplicate lots are not allowed
            }

            // Update height of nodes in backtracking/unwinding
            node.height = 1 + Math.max(height(node.left), height(node.right));

            // Check balance factor and perform rotations as the recursion unwinds
            int balance = getBalance(node);

            // Left-Left Case
            if (balance > 1 && newNode.getLoadCapacity() < node.left.getLoadCapacity()) {
                return rightRotate(node);
            }

            // Right-Right Case
            if (balance < -1 && newNode.getLoadCapacity() > node.right.getLoadCapacity()) {
                return leftRotate(node);
            }

            // Left-Right Case
            if (balance > 1 && newNode.getLoadCapacity() > node.left.getLoadCapacity()) {
                node.left = leftRotate(node.left);
                return rightRotate(node);
            }

            // Right-Left Case
            if (balance < -1 && newNode.getLoadCapacity() < node.right.getLoadCapacity()) {
                node.right = rightRotate(node.right);
                return leftRotate(node);
            }

            return node; // Return the balanced node
        }

        public void delete(int loadCapacity) {
            root = delete(root, loadCapacity);
            if (root != null) {
                root.parent = null; // The root's parent should be null
            }
        }

        private LedgerNode delete(LedgerNode node, int loadCapacity) {
            return delete(node, loadCapacity, true);
        }

        private LedgerNode delete(LedgerNode node, int loadCapacity, boolean removeFromLedgers) {
            if (node == null) {
                System.out.println("No deletion done, lot with capacity does not exist.");
                return null;
            }

            // Traverse the tree
            if (loadCapacity < node.getLoadCapacity()) {
                node.left = delete(node.left, loadCapacity, removeFromLedgers);
                if (node.left != null)
                    node.left.parent = node;
            } else if (loadCapacity > node.getLoadCapacity()) {
                node.right = delete(node.right, loadCapacity, removeFromLedgers);
                if (node.right != null)
                    node.right.parent = node;
            } else {
                // Node to be deleted found
                ParkingLot lotToDelete = node.lot; // Store the original lot

                if (removeFromLedgers) {
                    // Remove from ledgers before node.lot is potentially changed
                    removeFromLedgers(lotToDelete);
                }

                // Handle deletion cases
                // Case 1: No child
                if (node.left == null && node.right == null) {
                    System.out.println("Lot with capacity " + node.getLoadCapacity() + " has been deleted.");
                    return null;
                }
                // Case 2: One child
                else if (node.left == null || node.right == null) {
                    LedgerNode temp = (node.left != null) ? node.left : node.right;

                    temp.parent = node.parent;
                    System.out.println("Lot with capacity " + node.getLoadCapacity() + " has been deleted.");
                    return temp;
                }
                // Case 3: Two children
                else {
                    LedgerNode temp = minValueNode(node.right);

                    // Replace node's lot with successor's lot
                    node.lot = temp.lot;

                    // Delete the successor node
                    node.right = delete(node.right, temp.getLoadCapacity(), false);
                    if (node.right != null)
                        node.right.parent = node;
                }
            }

            // Update the height of the current node
            node.height = 1 + Math.max(height(node.left), height(node.right));

            // Balance the node if necessary
            int balance = getBalance(node);

            // Left-Left Case
            if (balance > 1 && getBalance(node.left) >= 0) {
                return rightRotate(node);
            }

            // Left-Right Case
            if (balance > 1 && getBalance(node.left) < 0) {
                node.left = leftRotate(node.left);
                if (node.left != null) node.left.parent = node;
                return rightRotate(node);
            }

            // Right-Right Case
            if (balance < -1 && getBalance(node.right) <= 0) {
                return leftRotate(node);
            }

            // Right-Left Case
            if (balance < -1 && getBalance(node.right) > 0) {
                node.right = rightRotate(node.right);
                if (node.right != null) node.right.parent = node;
                return leftRotate(node);
            }

            return node;
        }

        // Method to remove a lot from the ledgers
        private void removeFromLedgers(ParkingLot lotToDelete) {
            // Remove from waitingLedger if present
            if (lotToDelete.isInWaitingLedger) {
                waitingLedger.delete(lotToDelete.getLoadCapacity());
                lotToDelete.isInWaitingLedger = false;
                System.out.println("DELETE. Lot " + lotToDelete.getLoadCapacity() + " is deleted from the waiting ledger.");
            }

            // Remove from readyLedger if present
            if (lotToDelete.isInReadyLedger) {
                readyLedger.delete(lotToDelete.getLoadCapacity());
                lotToDelete.isInReadyLedger = false;
                System.out.println("DELETE. Lot " + lotToDelete.getLoadCapacity() + " is deleted from the ready ledger.");
            }
        }

        public LedgerNode minValueNode(LedgerNode node) {
            LedgerNode current = node;
            while (current.left != null) {
                current = current.left;
            }
            return current;
        }
    }

    // Node class for the ledger
    public class LedgerNode {
        int height;
        LedgerNode left;
        LedgerNode right;
        LedgerNode parent;
        ParkingLot lot; // Pointer to the parking lot that this node encapsulates

        public LedgerNode(ParkingLot lot) {
            this.lot = lot;
            this.height = 1; // Initial height for AVL node
        }

        public int getLoadCapacity() {
            return lot.getLoadCapacity();
        }
    }

    // Node class for the ready ledger
    public class ReadyNode {
        int height;
        ReadyNode left;
        ReadyNode right;
        ReadyNode parent;
        ParkingLot lot; // Pointer to the parking lot that this node encapsulates
        int totalTrucks;

        public ReadyNode(ParkingLot lot) {
            this.lot = lot;
            this.height = 1; // Initial height for AVL node
            this.totalTrucks = lot.readySection.getNumberOfTrucks();
        }

        // Getters and setters as needed
        public int getLoadCapacity() {
            return lot.getLoadCapacity();
        }
    }

    // AVL tree structure for parking lots with ready trucks
    public class ReadyLedger {
        private ReadyNode root;

        public ReadyNode findNode(int loadCapacity) {
            return findNode(root, loadCapacity);
        }

        private ReadyNode findNode(ReadyNode node, int loadCapacity) {
            if (node == null) {
                return null;
            }
            if (loadCapacity == node.lot.getLoadCapacity()) {
                return node;
            } else if (loadCapacity < node.lot.getLoadCapacity()) {
                return findNode(node.left, loadCapacity);
            } else {
                return findNode(node.right, loadCapacity);
            }
        }

        public ReadyNode findLotMax(int loadCapacity) {
            ReadyNode current = root;
            ReadyNode min = minValueNode(root);

            if (min != null && min.lot.load_capacity == loadCapacity) {
                return min;
            }

            while (current != null) {
                if (loadCapacity < current.lot.getLoadCapacity()) {
                    current = current.left;
                } else if (loadCapacity > current.lot.getLoadCapacity()) {
                    current = current.right;
                } else {
                    return current;
                }
            }

            return findClosestGreater(loadCapacity);  // If lot not found, return closest greater
        }

        // Method to find the closest parking lot with a load capacity greater than given value
        public ReadyNode findClosestGreater(int loadCapacity) {
            ReadyNode current = root;
            ReadyNode closestGreater = null;

            while (current != null) {
                if (current.lot.getLoadCapacity() > loadCapacity) {
                    // If current node's capacity is greater or equal, it could be our answer
                    closestGreater = current;
                    // Move left to find a potentially smaller but still valid capacity
                    current = current.left;
                } else {
                    // Move right to look for larger capacities
                    current = current.right;
                }
            }
            return closestGreater;
        }

        // Function to get the height of a node
        private int height(ReadyNode node) {
            return node == null ? 0 : node.height;
        }

        // Right rotation for balancing
        private ReadyNode rightRotate(ReadyNode y) {
            ReadyNode x = y.left;
            ReadyNode T2 = x.right;

            // Perform rotation
            x.right = y;
            y.left = T2;

            // Update parents
            if (T2 != null) {
                T2.parent = y;
            }
            x.parent = y.parent;
            y.parent = x;

            // Update the child pointer in the parent
            if (x.parent != null) {
                if (x.parent.left == y) {
                    x.parent.left = x;
                } else if (x.parent.right == y) {
                    x.parent.right = x;
                }
            } else {
                root = x; // x becomes the new root
            }

            // Update heights
            y.height = Math.max(height(y.left), height(y.right)) + 1;
            x.height = Math.max(height(x.left), height(x.right)) + 1;

            // Update totalTrucks
            y.totalTrucks = getTotalTrucks(y.left) + getTotalTrucks(y.right) + y.lot.readySection.getNumberOfTrucks();
            x.totalTrucks = getTotalTrucks(x.left) + getTotalTrucks(x.right) + x.lot.readySection.getNumberOfTrucks();


            return x; // New root after rotation
        }

        // Left rotation for balancing
        private ReadyNode leftRotate(ReadyNode x) {
            ReadyNode y = x.right;
            ReadyNode T2 = y.left;

            // Perform rotation
            y.left = x;
            x.right = T2;

            // Update parents
            if (T2 != null) {
                T2.parent = x;
            }
            y.parent = x.parent;
            x.parent = y;

            // Update the child pointer in the parent
            if (y.parent != null) {
                if (y.parent.left == x) {
                    y.parent.left = y;
                } else if (y.parent.right == x) {
                    y.parent.right = y;
                }
            } else {
                root = y; // y becomes the new root
            }

            // Update heights
            x.height = Math.max(height(x.left), height(x.right)) + 1;
            y.height = Math.max(height(y.left), height(y.right)) + 1;

            // Update totalTrucks
            x.totalTrucks = getTotalTrucks(x.left) + getTotalTrucks(x.right) + x.lot.readySection.getNumberOfTrucks();
            y.totalTrucks = getTotalTrucks(y.left) + getTotalTrucks(y.right) + y.lot.readySection.getNumberOfTrucks();

            return y; // New root after rotation
        }

        // Get balance factor of a node
        private int getBalance(ReadyNode node) {
            return node == null ? 0 : height(node.left) - height(node.right);
        }

        // Method to insert a parking lot to the ReadyLedger
        public void insert(ReadyNode newNode) {
            root = insert(root, newNode);
            if (root != null) {
                root.parent = null;
            }
        }

        // Internal recursive insert function with balancing
        private ReadyNode insert(ReadyNode node, ReadyNode newNode) {
            if (node == null) {
                System.out.println("Ready lot with capacity " + newNode.lot.getLoadCapacity() + " inserted into readyLedger");
                return newNode; // Insert new node at the correct position
            }

            // Recursion that traverses based on load capacity
            if (newNode.lot.getLoadCapacity() < node.lot.getLoadCapacity()) {
                node.left = insert(node.left, newNode);
                if (node.left != null)
                    node.left.parent = node;
            } else if (newNode.lot.getLoadCapacity() > node.lot.getLoadCapacity()) {
                node.right = insert(node.right, newNode);
                if (node.right != null)
                    node.right.parent = node;
            } else {
                return node; // Duplicate lots are not allowed
            }

            // Update height of nodes in backtracking/unwinding
            node.height = 1 + Math.max(height(node.left), height(node.right));
            node.totalTrucks = getTotalTrucks(node.left) + getTotalTrucks(node.right) + node.lot.readySection.getNumberOfTrucks();

            // Check balance factor and perform rotations as the recursion unwinds

            // Left-Left Case
            int balance = getBalance(node);
            if (balance > 1 && newNode.lot.getLoadCapacity() < node.left.lot.getLoadCapacity()) {
                return rightRotate(node);
            }

            // Right-Right Case
            if (balance < -1 && newNode.lot.getLoadCapacity() > node.right.lot.getLoadCapacity()) {
                return leftRotate(node);
            }

            // Left-Right Case
            if (balance > 1 && newNode.lot.getLoadCapacity() > node.left.lot.getLoadCapacity()) {
                node.left = leftRotate(node.left);
                return rightRotate(node);
            }

            // Right-Left Case
            if (balance < -1 && newNode.lot.getLoadCapacity() < node.right.lot.getLoadCapacity()) {
                node.right = rightRotate(node.right);
                return leftRotate(node);
            }

            return node; // Return the balanced node
        }

        public void delete(int loadCapacity) {
            root = delete(root, loadCapacity);
            if (root != null) {
                root.parent = null; // The root's parent should be null
            }
        }

        private ReadyNode delete(ReadyNode node, int loadCapacity) {
            if (node == null) {
                return null;
            }

            // Traverse the tree
            if (loadCapacity < node.lot.getLoadCapacity()) {
                node.left = delete(node.left, loadCapacity);
                if (node.left != null)
                    node.left.parent = node;
            } else if (loadCapacity > node.lot.getLoadCapacity()) {
                node.right = delete(node.right, loadCapacity);
                if (node.right != null)
                    node.right.parent = node;
            } else {
                // Node to be deleted found

                // Case 1: No child
                if (node.left == null && node.right == null) {
                    return null;
                }
                // Case 2: One child (right child only)
                else if (node.left == null) {
                    node.right.parent = node.parent;
                    return node.right;
                }
                // Case 2: One child (left child only)
                else if (node.right == null) {
                    node.left.parent = node.parent;
                    return node.left;
                }
                // Case 3: Two children
                else {
                    ReadyNode temp = minValueNode(node.right);

                    // Copy data
                    node.lot = temp.lot;

                    // Delete successor
                    node.right = delete(node.right, temp.lot.load_capacity);
                    if (node.right != null)
                        node.right.parent = node;
                }
            }

            // If the node is null after deletion, return null
            if (node == null) {
                return null;
            }

            // Update the height of the current node
            node.height = 1 + Math.max(height(node.left), height(node.right));
            node.totalTrucks = getTotalTrucks(node.left) + getTotalTrucks(node.right) + node.lot.readySection.getNumberOfTrucks();


            // Get the balance factor to check if this node became unbalanced
            int balance = getBalance(node);

            // Balance the node if necessary

            // Left-Left Case
            if (balance > 1 && getBalance(node.left) >= 0) {
                return rightRotate(node);
            }

            // Left-Right Case
            if (balance > 1 && getBalance(node.left) < 0) {
                node.left = leftRotate(node.left);
                return rightRotate(node);
            }

            // Right-Right Case
            if (balance < -1 && getBalance(node.right) <= 0) {
                return leftRotate(node);
            }

            // Right-Left Case
            if (balance < -1 && getBalance(node.right) > 0) {
                node.right = rightRotate(node.right);
                return leftRotate(node);
            }

            // Return the balanced node
            return node;
        }

        // Method to find the node with the smallest load capacity in a subtree
        public ReadyNode minValueNode(ReadyNode node) {
            ReadyNode current = node;
            while (current != null && current.left != null) {
                current = current.left;
            }
            return current;
        }
    }

    // Node class for waiting ledger
    public class WaitingNode {
        int height;
        WaitingNode left;
        WaitingNode right;
        WaitingNode parent;
        ParkingLot lot; // Pointer to the parking lot that this node encapsulates
        int totalTrucks;

        public WaitingNode(ParkingLot lot) {
            this.lot = lot;
            this.height = 1; // Initial height for AVL node
            this.totalTrucks = lot.waitingSection.getNumberOfTrucks();
        }

        // Getters and setters as needed
        public int getLoadCapacity() {
            return lot.getLoadCapacity();
        }
    }

    public class WaitingLedger {
        private WaitingNode root;

        public WaitingNode findNode(int loadCapacity) {
            return findNode(root, loadCapacity);
        }

        private WaitingNode findNode(WaitingNode node, int loadCapacity) {
            if (node == null) {
                return null;
            }
            if (loadCapacity == node.lot.getLoadCapacity()) {
                return node;
            } else if (loadCapacity < node.lot.getLoadCapacity()) {
                return findNode(node.left, loadCapacity);
            } else {
                return findNode(node.right, loadCapacity);
            }
        }


        public void insert(WaitingNode newNode) {
            root = insert(root, newNode);
            if (root != null) {
                root.parent = null;
            }
        }

        private WaitingNode insert(WaitingNode node, WaitingNode newNode) {
            if (node == null) {
                return newNode;
            }

            if (newNode.getLoadCapacity() < node.getLoadCapacity()) {
                node.left = insert(node.left, newNode);
                node.left.parent = node;
            } else if (newNode.getLoadCapacity() > node.getLoadCapacity()) {
                node.right = insert(node.right, newNode);
                node.right.parent = node;
            } else {
                return node; // Duplicate capacities are not allowed
            }

            // Update height and balance
            node.height = 1 + Math.max(height(node.left), height(node.right));
            node.totalTrucks = getTotalTrucks(node.left) + getTotalTrucks(node.right) + node.lot.waitingSection.getNumberOfTrucks();
            return balance(node);
        }

        public void delete(int loadCapacity) {
            root = delete(root, loadCapacity);
            if (root != null) {
                root.parent = null;
            }
        }

        private WaitingNode delete(WaitingNode node, int loadCapacity) {
            if (node == null) {
                return null;
            }

            if (loadCapacity < node.getLoadCapacity()) {
                node.left = delete(node.left, loadCapacity);
                if (node.left != null)
                    node.left.parent = node;
            } else if (loadCapacity > node.getLoadCapacity()) {
                node.right = delete(node.right, loadCapacity);
                if (node.right != null)
                    node.right.parent = node;
            } else {
                // Node to be deleted found
                if (node.left == null || node.right == null) {
                    WaitingNode temp = (node.left != null) ? node.left : node.right;

                    if (temp == null) {
                        node = null;
                    } else {
                        temp.parent = node.parent;
                        node = temp;
                    }
                } else {
                    WaitingNode temp = minValueNode(node.right);
                    node.lot = temp.lot;
                    node.right = delete(node.right, temp.getLoadCapacity());
                    if (node.right != null)
                        node.right.parent = node;
                }
            }

            if (node == null) {
                return node;
            }

            // Update height and balance
            node.height = Math.max(height(node.left), height(node.right)) + 1;
            node.totalTrucks = getTotalTrucks(node.left) + getTotalTrucks(node.right) + node.lot.waitingSection.getNumberOfTrucks();

            return balance(node);
        }

        private int height(WaitingNode node) {
            return node == null ? 0 : node.height;
        }

        private int getBalance(WaitingNode node) {
            return node == null ? 0 : height(node.left) - height(node.right);
        }

        private WaitingNode balance(WaitingNode node) {
            int balance = getBalance(node);

            // Left Left Case
            if (balance > 1 && getBalance(node.left) >= 0) {
                return rightRotate(node);
            }

            // Left Right Case
            if (balance > 1 && getBalance(node.left) < 0) {
                node.left = leftRotate(node.left);
                return rightRotate(node);
            }

            // Right Right Case
            if (balance < -1 && getBalance(node.right) <= 0) {
                return leftRotate(node);
            }

            // Right Left Case
            if (balance < -1 && getBalance(node.right) > 0) {
                node.right = rightRotate(node.right);
                return leftRotate(node);
            }

            return node;
        }

        private WaitingNode rightRotate(WaitingNode y) {
            WaitingNode x = y.left;
            WaitingNode T2 = x.right;

            // Perform rotation
            x.right = y;
            y.left = T2;

            // Update parents
            if (T2 != null) {
                T2.parent = y;
            }
            x.parent = y.parent;
            y.parent = x;

            // Update heights
            y.height = Math.max(height(y.left), height(y.right)) + 1;
            x.height = Math.max(height(x.left), height(x.right)) + 1;

            // Update totalTrucks
            y.totalTrucks = getTotalTrucks(y.left) + getTotalTrucks(y.right) + y.lot.waitingSection.getNumberOfTrucks();
            x.totalTrucks = getTotalTrucks(x.left) + getTotalTrucks(x.right) + x.lot.waitingSection.getNumberOfTrucks();


            return x;
        }

        private WaitingNode leftRotate(WaitingNode x) {
            WaitingNode y = x.right;
            WaitingNode T2 = y.left;

            // Perform rotation
            y.left = x;
            x.right = T2;

            // Update parents
            if (T2 != null) {
                T2.parent = x;
            }
            y.parent = x.parent;
            x.parent = y;

            // Update heights
            x.height = Math.max(height(x.left), height(x.right)) + 1;
            y.height = Math.max(height(y.left), height(y.right)) + 1;

            // Update totalTrucks
            x.totalTrucks = getTotalTrucks(x.left) + getTotalTrucks(x.right) + x.lot.waitingSection.getNumberOfTrucks();
            y.totalTrucks = getTotalTrucks(y.left) + getTotalTrucks(y.right) + y.lot.waitingSection.getNumberOfTrucks();


            return y;
        }

        private WaitingNode minValueNode(WaitingNode node) {
            WaitingNode current = node;
            while (current.left != null) {
                current = current.left;
            }
            return current;
        }

        public WaitingNode findLot(int loadCapacity) {
            WaitingNode current = root;
            WaitingNode closestGreater = null;

            while (current != null) {
                if (current.getLoadCapacity() >= loadCapacity) {
                    closestGreater = current;
                    current = current.left;
                } else {
                    current = current.right;
                }
            }
            return closestGreater;
        }
    }

    private int getTotalTrucks(WaitingNode node) {
        return node == null ? 0 : node.totalTrucks;
    }

    private int getTotalTrucks(ReadyNode node) {
        return node == null ? 0 : node.totalTrucks;
    }

}
