package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);

        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();

            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }


    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }



    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Login failed.");

            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }

    }

    private static void loginCaregiver(String[] tokens) {

        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }

        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2

        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        } else if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        } else {
            String date = tokens[1];
            if(!existsDate(date)) {
                System.out.println("there is no available caregivers at this date");
            }

            try{
                ConnectionManager cm = new ConnectionManager();
                Connection con = cm.createConnection();

                String selectCaregivers = "SELECT DISTINCT A.Username " +
                        "FROM Availabilities A " +
                        "WHERE A.Time = ? " +
                        "ORDER BY A.Username";

                PreparedStatement statement = con.prepareStatement(selectCaregivers);
                statement.setString(1, date);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String caregiverUsername = resultSet.getString("Username");


                    String selectVaccineInfo = "SELECT Name, Doses FROM Vaccines";
                    PreparedStatement vaccineStatement = con.prepareStatement(selectVaccineInfo);
                    ResultSet vaccineResultSet = vaccineStatement.executeQuery();

                    System.out.print("Caregiver: " + caregiverUsername + ", ");


                    while (vaccineResultSet.next()) {
                        String vaccineName = vaccineResultSet.getString("Name");
                        int availableDoses = vaccineResultSet.getInt("Doses");

                        System.out.print("Vaccine: " + vaccineName +
                                ", Available Doses: " + availableDoses + ", ");
                    }

                    System.out.println();
                }

                    cm.closeConnection();



            } catch (SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
            }

            }

        }





    private static boolean existsDate(String date) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT Username FROM Availabilities WHERE Time = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, date);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking date");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        } else if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        } else if (currentPatient == null){
            System.out.println("Please login as a patient!");
            return;
        }

        String date = tokens[1];
        String vaccineName = tokens[2];

        try {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();


            // Find available caregivers for the given date
            String selectCaregivers = "SELECT DISTINCT A.Username " +
                    "FROM Availabilities A " +
                    "WHERE A.Time = ? " +
                    "ORDER BY A.Username";

            PreparedStatement statement = con.prepareStatement(selectCaregivers);
            statement.setString(1, date);
            ResultSet resultSet = statement.executeQuery();

            if(!resultSet.next()) {
                System.out.println("no caregiver available");
                return;
            }
            String caregiverUsername = resultSet.getString("Username");

            // Check if caregiver has an appointment for the given date

                // Check if there are enough vaccine doses available
                Vaccine vaccine = new Vaccine.VaccineGetter(vaccineName).get();
                if (vaccine == null ) {
                    System.out.println("No Vaccine!");
                    return;
                } else if(vaccine.getAvailableDoses() == 0) {
                        // Not enough available doses
                        System.out.println("Not enough available doses!");
                        return;
                }
                    // Decrease available vaccine doses
                    vaccine.decreaseAvailableDoses(1);

                    // Generate a unique appointment ID
                    String appointmentId = Util.generateUUID();

                    // Insert the appointment into the Appointments table
                    String insertAppointment = "INSERT INTO Appointments VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement appointmentStatement = con.prepareStatement(insertAppointment);
                    appointmentStatement.setString(1, appointmentId);
                    appointmentStatement.setDate(2, Date.valueOf(date));
                    appointmentStatement.setString(3, currentPatient.getUsername());
                    appointmentStatement.setString(4, caregiverUsername);
                    appointmentStatement.setString(5, vaccineName);
                    appointmentStatement.executeUpdate();

                    System.out.println("Appointment ID: " + appointmentId + ", Caregiver username: " + caregiverUsername);
                    String deleteTable = "DELETE FROM Availabilities WHERE Username = ? AND Time = ?";
                    PreparedStatement deleteStatement = con.prepareStatement(deleteTable);
                    deleteStatement.setString(1,caregiverUsername);
                    deleteStatement.setDate(2,Date.valueOf(date));
                    deleteStatement.executeUpdate();

                    cm.closeConnection();
                    return;

        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        }
    }




    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit

        // Check if the current user is a caregiver or patient
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }

        // Check if the correct number of arguments is provided
        if (tokens.length != 2) {
            System.out.println("Please provide the appointment ID to cancel.");
            return;
        }

        // Get the appointment ID from the tokens
        String appointmentId = tokens[1];

        try {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            // Check if the appointment exists
            String selectAppointment = "SELECT * FROM Appointments WHERE aid = ?";
            PreparedStatement appointmentStatement = con.prepareStatement(selectAppointment);
            appointmentStatement.setString(1, appointmentId);
            ResultSet resultSet = appointmentStatement.executeQuery();

            if (!resultSet.next()) {
                System.out.println("Appointment with ID " + appointmentId + " not found.");
                cm.closeConnection();
                return;
            }

            // Check if the appointment belongs to the logged-in caregiver or patient
            String caregiverUsername = resultSet.getString("cid");
            String patientUsername = resultSet.getString("pid");

            if ((currentCaregiver != null && currentCaregiver.getUsername().equals(caregiverUsername)) ||
                    (currentPatient != null && currentPatient.getUsername().equals(patientUsername))) {


                // Delete the appointment from the Appointments table
                String deleteAppointment = "DELETE FROM Appointments WHERE aid = ?";
                PreparedStatement deleteStatement = con.prepareStatement(deleteAppointment);
                deleteStatement.setString(1, appointmentId);
                deleteStatement.executeUpdate();

                String addBackAv = "INSERT INTO Availabilities VALUES(?, ?)";
                PreparedStatement addStatement = con.prepareStatement(addBackAv);
                addStatement.setDate(1, resultSet.getDate("Time"));
                addStatement.setString(2, resultSet.getString("cid"));
                addStatement.executeUpdate();

                // Increase available doses for the canceled appointment
                String vaccineName = resultSet.getString("vid");
                int canceledDoses = 1;
                Vaccine vaccine = new Vaccine.VaccineGetter(vaccineName).get();
                if (vaccine != null) {
                    vaccine.increaseAvailableDoses(canceledDoses);
                }

                System.out.println("Appointment with ID " + appointmentId + " canceled successfully.");
            } else {
                System.out.println("You don't have permission to cancel this appointment.");
                return;
            }

            cm.closeConnection();

        } catch (SQLException e) {
            System.out.println("Error occurred while canceling appointment.");
            e.printStackTrace();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
         if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }

        try{
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            if(currentPatient != null) {

                String patientUsername = currentPatient.getUsername();

                String selectAppoinment = "SELECT A.aid, A.vid, A.Time, A.cid " +
                        "FROM Appointments A " +
                        "WHERE A.pid = ? " +
                        "ORDER BY A.aid";

                PreparedStatement statement = con.prepareStatement(selectAppoinment);
                statement.setString(1, patientUsername);
                ResultSet resultSet = statement.executeQuery();

                if(!appointmentExistsPatient(patientUsername)) {
                    System.out.println("no appointment was reserved");
                    return;
                }

                while(resultSet.next()) {
                    System.out.println("Appointment ID: " + resultSet.getString("aid") + " "
                            + "vaccine name: " + resultSet.getString("vid") + " "
                            + "date: " + resultSet.getString("Time") + " "
                            + "caregiver name: " + resultSet.getString("cid"));
                }

            }

            if(currentCaregiver != null) {
                String caregiverUsername = currentCaregiver.getUsername();

                String selectAppoinment = "SELECT A.aid, A.vid, A.Time, A.pid " +
                        "FROM Appointments A " +
                        "WHERE A.cid = ? " +
                        "ORDER BY A.aid";

                PreparedStatement statement = con.prepareStatement(selectAppoinment);
                statement.setString(1, caregiverUsername);
                ResultSet resultSet = statement.executeQuery();

                if(!appointmentExistsCare(caregiverUsername)) {
                    System.out.println("no appointment was reserved");
                    return;
                }

                while(!resultSet.next()) {
                    System.out.println("Appointment ID: " + resultSet.getString("aid") + " "
                            + "vaccine name: " + resultSet.getString("vid") + " "
                            + "date: " + resultSet.getString("Time") + " "
                            + "patient name: " + resultSet.getString("pid"));
                }


            }



            cm.closeConnection();



        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        }

    }

    private static boolean appointmentExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Appointments WHERE pid = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean appointmentExistsCare(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Appointments WHERE cid = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }
    private static void logout(String[] tokens) {
        // TODO: Part 2
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
        } else {
            if (currentCaregiver != null) {
                System.out.println("Successfully logged out!: " + currentCaregiver.getUsername());
                currentCaregiver = null;
            } else if (currentPatient != null) {
                System.out.println("Successfully logged out!: " + currentPatient.getUsername());
                currentPatient = null;
            } else {
                System.out.println("Please try again!");
            }
        }
    }
}
