import java.io.*;

public class Main {

    public static void main(String[] args) {
        ParkingLotManager manager = new ParkingLotManager();

        String inputFile = args[0];
        String outputFile = args[1];

        // Use try-with-resources to ensure files are closed
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            // Read each line (command) from the input file
            while ((line = reader.readLine()) != null) {
                // Trim and ignore empty lines or comments
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Split the command into parts
                String[] parts = line.split("\\s+");
                String command = parts[0].toLowerCase();

                String output = ""; // To collect the output for this command

                switch (command) {
                    case "create_parking_lot":
                        handleCreateParkingLot(manager, parts);
                        break;

                    case "add_truck":
                        output = handleAddTruck(manager, parts);
                        break;

                    case "ready":
                        output = handleReady(manager, parts);
                        break;

                    case "load":
                        // Handle LOAD command
                        output = handleLoad(manager, parts);
                        break;

                    case "count":
                        // Handle COUNT command
                        output = handleCount(manager, parts);
                        break;

                    case "delete_parking_lot":
                        // Handle delete_parking_lot command
                        handleDeleteLot(manager, parts);
                        break;

                    default:
                        output = "Unknown command: " + command;
                        break;
                }

                if (!output.isEmpty()) {
                    writer.write(output);
                    writer.newLine(); // Add a newline for readability
                }
            }

            System.out.println("Simulation completed. Outputs are written to " + outputFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Create Parking Lot
    private static void handleCreateParkingLot(ParkingLotManager manager, String[] parts) {
        int loadCapacity = Integer.parseInt(parts[1]);
        int truckCapacity = Integer.parseInt(parts[2]);
        manager.ledger.insert(manager.new ParkingLot(loadCapacity, truckCapacity));
    }

    // Add Truck
    private static String handleAddTruck(ParkingLotManager manager, String[] parts) {
        int truckID = Integer.parseInt(parts[1]);
        int totalCapacity = Integer.parseInt(parts[2]);
        int result = manager.truckAdder(truckID, totalCapacity);

        if (result == -2) {
            result = -1;
        }

        return String.valueOf(result);
    }

    // Ready Command
    private static String handleReady(ParkingLotManager manager, String[] parts) {
        int capacity = Integer.parseInt(parts[1]);
        ParkingLotManager.WaitingNode lot = manager.waitingLedger.findLot(capacity);

        if (lot == null)
            return "-1";
        else {
            int[] result = lot.lot.ready();
            return String.valueOf(result[0]) + " " + String.valueOf(result[1]);
        }
    }

    // Load Command
    private static String handleLoad(ParkingLotManager manager, String[] parts) {
        int capacity = Integer.parseInt(parts[1]);
        int amount = Integer.parseInt(parts[2]);
        String output;

        ParkingLotManager.ReadyNode lot = manager.readyLedger.findLotMax(capacity);

        if (lot == null) {
            output = "-1";
        } else {
            output = manager.load(lot, amount);
        }

        return output;
    }

    // Delete Parking Lot
    private static void handleDeleteLot(ParkingLotManager manager, String[] parts) {
        int capacity = Integer.parseInt(parts[1]);
        manager.ledger.delete(capacity);
    }

    // Count Command
    private static String handleCount(ParkingLotManager manager, String[] parts) {
        int capacity = Integer.parseInt(parts[1]);
        int result = manager.countTrucks(capacity);

        return String.valueOf(result);
    }
}
