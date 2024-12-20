import java.io.File;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;

class Dispatcher {

    private static Processus[] processusTableau; // Attribut de classe pour le tableau de processus
    private static JTextArea outputFileArea; // Zone pour afficher le fichier de sortie

    private static String buildBaseLine(Processus[] processusTableau) {
        StringBuilder baseLineBuilder = new StringBuilder();
        for (Processus p : processusTableau) {
            baseLineBuilder.append(p.getStateProcString()).append(" ");
        }
        return baseLineBuilder.toString().trim();
    }

    private static Processus pickHighestPriorityProcess(Map<Integer, LinkedList<Processus>> priorityQueues) {
        for (Map.Entry<Integer, LinkedList<Processus>> entry : priorityQueues.entrySet()) {
            LinkedList<Processus> queue = entry.getValue();
            if (!queue.isEmpty()) {
                return queue.poll();
            }
        }
        return null; // aucune file n'a de processus
    }

    public static void main(String[] args) {
        IOCommandes ecran = new IOCommandes();

        JFrame frame = new JFrame("Ordonnancement des Processus");
        frame.setSize(1600, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Liste des fichiers disponibles
        String[] fileOptions = {"processus_1.txt", "processus_2.txt", "processus_TD.txt"};
        JList<String> fileList = new JList<>(fileOptions);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Zone pour afficher le contenu du fichier sélectionné
        JTextArea fileContentArea = new JTextArea();
        fileContentArea.setEditable(false);
        JScrollPane fileContentScrollPane = new JScrollPane(fileContentArea);

        // Zone pour afficher le fichier de sortie
        outputFileArea = new JTextArea();
        outputFileArea.setEditable(false);
        JScrollPane outputFileScrollPane = new JScrollPane(outputFileArea);

        // SplitPane pour afficher les deux zones côte à côte
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fileContentScrollPane, outputFileScrollPane);
        splitPane.setDividerLocation(800);

        // Bouton pour confirmer la sélection de fichier
        JButton selectFileButton = new JButton("Afficher Contenu");

        // Panneau de sélection des fichiers
        JPanel filePanel = new JPanel(new BorderLayout());
        filePanel.add(new JLabel("Sélectionnez un fichier :"), BorderLayout.NORTH);
        filePanel.add(new JScrollPane(fileList), BorderLayout.CENTER);
        filePanel.add(selectFileButton, BorderLayout.SOUTH);

        // Panneau principal
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(filePanel, BorderLayout.WEST);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Panneau pour sélectionner le type d'ordonnancement
        JPanel schedulingPanel = new JPanel(new BorderLayout());
        JLabel schedulingLabel = new JLabel("Sélectionnez une méthode d'ordonnancement :");
        String[] schedulingOptions = {
                "FIFO sans I/O sans priorité",
                "FIFO avec priorité et I/O",
                "Round Robin sans priorité",
                "Round Robin avec priorité"
        };
        JComboBox<String> schedulingComboBox = new JComboBox<>(schedulingOptions);
        JButton startSchedulingButton = new JButton("Démarrer Ordonnancement");

        schedulingPanel.add(schedulingLabel, BorderLayout.NORTH);
        schedulingPanel.add(schedulingComboBox, BorderLayout.CENTER);
        schedulingPanel.add(startSchedulingButton, BorderLayout.SOUTH);
        schedulingPanel.setVisible(false);

        mainPanel.add(schedulingPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);

        // Action pour charger un fichier
        selectFileButton.addActionListener(e -> {
            String selectedFileName = fileList.getSelectedValue();
            if (selectedFileName == null) {
                JOptionPane.showMessageDialog(frame, "Veuillez sélectionner un fichier.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            File selectedFile = new File("./files/" + selectedFileName);
            try {
                String data = ecran.lireFile(selectedFile);
                fileContentArea.setText(data);

                // Créer le tableau des processus
                processusTableau = ecran.tableProcess(data);
                JOptionPane.showMessageDialog(frame, "Fichier chargé avec succès.");
                schedulingPanel.setVisible(true);
            } catch (Exception ex) {
                fileContentArea.setText("Erreur lors de la lecture du fichier : " + ex.getMessage());
            }
        });

        // Action pour démarrer l'ordonnancement
        startSchedulingButton.addActionListener(e -> {
            if (processusTableau == null) {
                JOptionPane.showMessageDialog(frame, "Veuillez d'abord charger un fichier.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int schedulingChoice = schedulingComboBox.getSelectedIndex() + 1;
            String outputFileName = ""; // Nom du fichier de sortie

            switch (schedulingChoice) {
                case 1:
                    outputFileName = ordonnancementFIFOWithoutIONoPriority(processusTableau, ecran);
                    break;
                case 2:
                    outputFileName = ordonnancementIOPriority(processusTableau, ecran);
                    break;
                case 3:
                    outputFileName = ordonnancementRoundRobinWithIONoPriority(processusTableau, ecran);
                    break;
                case 4:
                    outputFileName = ordonnancementRoundRobinWithIOPriority(processusTableau, ecran);
                    break;
                default:
                    JOptionPane.showMessageDialog(frame, "Choix d'ordonnancement invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    return;
            }

            // Lire et afficher le fichier de sortie
            try {
                File outputFile = new File(outputFileName);
                String resultContent = ecran.lireFile(outputFile);
                outputFileArea.setText(resultContent);
            } catch (Exception ex) {
                outputFileArea.setText("Erreur lors de la lecture du fichier de sortie : " + ex.getMessage());
            }
        });

        frame.setVisible(true);
    }

    private static int initializeAndPrepareProcesses(Processus[] processusTableau) {
        float minArrive = Float.MAX_VALUE;
        List<Processus> candidats = new ArrayList<>();
        Processus plusImportant = null;
        int plusFaiblePriorite = Integer.MAX_VALUE;

        // Trouver le temps d'arrivée minimal
        for (Processus p : processusTableau) {
            if (p.getArrive_t() < minArrive) {
                minArrive = p.getArrive_t();
            }
        }

        // Collecter les processus candidats
        for (Processus p : processusTableau) {
            if (p.getArrive_t() == minArrive) {
                candidats.add(p);
            }
        }

        // Trouver le processus avec la plus haute priorité
        for (Processus p : candidats) {
            if (p.getPriority_l() < plusFaiblePriorite) {
                plusFaiblePriorite = p.getPriority_l();
                plusImportant = p;
            }
        }

        // Configurer les processus
        if (plusImportant != null) {
            plusImportant.setActif(false);
            plusImportant.setStateProcString("a");
            plusImportant.setArrived(true);
            for (Processus p : candidats) {
                if (p != plusImportant) {
                    p.setArrived(true);
                    p.setStateProcString("a");
                }
            }
        }

        return (int) minArrive;
    }

    private static String ordonnancementFIFOWithoutIONoPriority(Processus[] processusTableau, IOCommandes ecran) {
        int nbProcessus = processusTableau.length;
        int startTime = 0;

        // Initialisation et configuration des processus
        if (nbProcessus > 0) {
            startTime = initializeAndPrepareProcesses(processusTableau);
        }

        // Affichage initial
        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        // Largeur de colonne
        int maxNameLength = 1;
        for (Processus p : processusTableau) {
            if (p.getNameProc().length() > maxNameLength) {
                maxNameLength = p.getNameProc().length();
            }
        }
        int stateLength = 8;
        int columnWidth = Math.max(maxNameLength, stateLength) + 7;

        // Préparer l'en-tête pour l'affichage
        StringBuilder header = new StringBuilder();
        header.append(String.format("%-" + columnWidth + "s", "X"));
        for (Processus p : processusTableau) {
            header.append(String.format("%-" + columnWidth + "s", p.getNameProc()));
        }

        String outputFileName = "etat_processus_fifo_basique.txt";
        try {
            new File(outputFileName).delete();
            new File(outputFileName).createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ecran.ecrireFile(outputFileName, header.toString());

        String previousBaseLine = null;
        String[] previousStates = new String[processusTableau.length];
        Arrays.fill(previousStates, "");

        int ts = startTime;
        while (!allProcessesFinished(processusTableau) && ts < startTime + 300) {
            // Activer les processus qui arrivent
            for (Processus p : processusTableau) {
                if (p.getArrive_t() == ts && !p.isArrived()) {
                    p.setArrived(true);
                    if (!p.isFinished()) {
                        p.setStateProcString("a");
                    }
                }
            }

            // Trouver un processus actif
            Processus actif = null;
            for (Processus p : processusTableau) {
                if (p.isActif()) {
                    actif = p;
                    break;
                }
            }

            // Si aucun actif, prendre le premier prêt
            if (actif == null) {
                for (Processus p : processusTableau) {
                    if (!p.isFinished() && p.isArrived()) {
                        actif = p;
                        break;
                    }
                }

                if (actif != null) {
                    actif.setActif(true);
                    actif.setStateProcString("A");
                }
            }

            // Exécuter le processus actif
            if (actif != null) {
                if (actif.getRemain_t() == 0) {
                    actif.setActif(false);
                    actif.setFinished(true);
                    actif.setStateProcString("X");

                    // Chercher un autre prêt
                    Processus suivant = null;
                    for (Processus p : processusTableau) {
                        if (!p.isFinished() && p.isArrived()) {
                            suivant = p;
                            break;
                        }
                    }

                    if (suivant != null) {
                        suivant.setActif(true);
                        suivant.setStateProcString("A");
                        // pas de "continue;" nécessaire car on va passer au writeStateLine plus bas
                    }
                }
            }

            //quantumCounter = writeStateLine(processusTableau, ts, columnWidth, previousStates, previousBaseLine, outputFileName, ecran, 1, quantumCounter);
            String baseLine = buildBaseLine(processusTableau);
            previousBaseLine = baseLine;
            ts++;
            if (actif != null && !actif.isFinished()) {
                actif.oneTimeSlice();
            }
        }

        File etatFichier = new File(outputFileName);
        String statesData = ecran.lireFile(etatFichier);
        System.out.println("\n=== Tableau des états (FIFO_IO) ===");
        System.out.print(statesData);

        System.out.println("\nOrdonnancement FIFO terminé.");

        return outputFileName;
    }

    private static String ordonnancementIOPriority(Processus[] processusTableau, IOCommandes ecran) {
        int nbProcessus = processusTableau.length;
        int startTime = 0;

        // Initialisation et configuration des processus
        if (nbProcessus > 0) {
            startTime = initializeAndPrepareProcesses(processusTableau);
        }

        // Affichage initial
        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        // Largeur de colonne
        int maxNameLength = 1;
        for (Processus p : processusTableau) {
            if (p.getNameProc().length() > maxNameLength) {
                maxNameLength = p.getNameProc().length();
            }
        }
        int stateLength = 8;
        int columnWidth = Math.max(maxNameLength, stateLength) + 7;

        // Préparer l'en-tête pour l'affichage
        StringBuilder header = new StringBuilder();
        header.append(String.format("%-" + columnWidth + "s", "X"));
        for (Processus p : processusTableau) {
            header.append(String.format("%-" + columnWidth + "s", p.getNameProc()));
        }

        String outputFileName = "etat_processus_fifo_prio_io.txt";
        try {
            new File(outputFileName).delete();
            new File(outputFileName).createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ecran.ecrireFile(outputFileName, header.toString());

        // État des processus
        String previousBaseLine = null;
        String[] previousStates = new String[processusTableau.length];
        Arrays.fill(previousStates, "");
        int quantumCounter = 0;
        int ts = startTime;

        // Simulation de l'ordonnancement
        while (!allProcessesFinished(processusTableau) && ts < startTime + 300) {
            for (Processus p : processusTableau) {
                if (p.isActif() && p.getRemain_t() > 0) {
                    p.oneTimeSlice();
                    quantumCounter++;
                    if (p.getRemain_t() == 0 && !p.isFinished()) {
                        quantumCounter = 0;
                        p.setActif(false);
                        p.setFinished(true);
                        p.setStateProcString("X");
                    }
                }

                if (p.getStateProcString().equals("B") && p.getIoLastF_t() != 0) {
                    p.oneBlockTimeSlice();
                    if (p.getIoLastF_t() == 0) {
                        p.setBlocked(false);
                        if (!p.isFinished()) p.setStateProcString("a");
                    }
                }

                if (!p.isFinished() && !p.isBlocked() &&
                        (p.getTotal_t() - p.getRemain_t()) == p.getIoAt_t() &&
                        p.getIoLastF_t() != 0) {
                    p.setActif(false);
                    p.setBlocked(true);
                    quantumCounter = 0;
                    p.setStateProcString("B");
                }

                if (p.getArrive_t() == ts && !p.isArrived()) {
                    p.setArrived(true);
                    if (!p.isFinished() && !p.isBlocked() && !p.isActif()) {
                        p.setStateProcString("a");
                    }
                }
            }

            // Choisir le processus le plus important
            Processus plusImportant = null;
            int plusFaiblePriorite = Integer.MAX_VALUE;
            for (Processus p : processusTableau) {
                if (!p.isFinished() && !p.isBlocked() && p.isArrived()) {
                    if (p.getPriority_l() < plusFaiblePriorite) {
                        plusFaiblePriorite = p.getPriority_l();
                        plusImportant = p;
                    }
                }
            }

            if (plusImportant != null) {
                for (Processus p : processusTableau) {
                    if (p != plusImportant && p.isActif()) {
                        p.setActif(false);
                        if (!p.isFinished() && !p.isBlocked()) {
                            p.setStateProcString("a");
                        }
                    }
                }
                plusImportant.setActif(true);
                plusImportant.setStateProcString("A");
                plusImportant.setArrived(true);
            }

            writeStateLine(processusTableau, ts, columnWidth, previousStates, previousBaseLine, outputFileName, ecran, 1, quantumCounter);
            String baseLine = buildBaseLine(processusTableau);
            previousBaseLine = baseLine;
            ts++;
        }

        // Afficher les processus restants
        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        File etatFichier = new File(outputFileName);
        String statesData = ecran.lireFile(etatFichier);
        System.out.println("\n=== Tableau des états (FIFO_IO_PRIO) ===");
        System.out.print(statesData);

        System.out.println("\nOrdonnancement par priorité terminé.");

        return outputFileName;
    }

    private static String ordonnancementRoundRobinWithIONoPriority(Processus[] processusTableau, IOCommandes ecran) {
        int nbProcessus = processusTableau.length;
        int startTime = 0;

        // Initialisation et configuration des processus
        if (nbProcessus > 0) {
            startTime = initializeAndPrepareProcesses(processusTableau);
        }

        // Affichage initial
        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        // Largeur de colonne
        int maxNameLength = 1;
        for (Processus p : processusTableau) {
            if (p.getNameProc().length() > maxNameLength) {
                maxNameLength = p.getNameProc().length();
            }
        }
        int stateLength = 8;
        int columnWidth = Math.max(maxNameLength, stateLength) + 7;

        // Préparer l'en-tête pour l'affichage
        StringBuilder header = new StringBuilder();
        header.append(String.format("%-" + columnWidth + "s", "X"));
        for (Processus p : processusTableau) {
            header.append(String.format("%-" + columnWidth + "s", p.getNameProc()));
        }

        String outputFileName = "etat_processus_rr_io.txt";
        try {
            new File(outputFileName).delete();
            new File(outputFileName).createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ecran.ecrireFile(outputFileName, header.toString());

        int ts = startTime;
        int maxSteps = 10;
        int quantumCounter = 0;
        LinkedList<Processus> readyQueue = new LinkedList<>();

        for (Processus p : processusTableau) {
            if (p.isArrived() && !p.isFinished() && !p.isBlocked()) {
                readyQueue.add(p);
            }
        }

        Processus currentProcess = null;
        String previousBaseLine = null;
        String[] previousStates = new String[processusTableau.length];
        Arrays.fill(previousStates, "");

        while (!allProcessesFinished(processusTableau) && ts < startTime + 300) {
            // Arrivée de nouveaux processus
            for (Processus p : processusTableau) {
                if ((int)p.getArrive_t() == ts && !p.isArrived()) {
                    p.setArrived(true);
                    p.setStateProcString("a");
                    readyQueue.add(p);
                }
            }

            // Fin d’I/O
            for (Processus p : processusTableau) {
                if (p.isBlocked()) {
                    p.oneBlockTimeSlice();
                    if (p.getIoLastF_t() == 0) {
                        p.setBlocked(false);
                        p.setStateProcString("a");
                        readyQueue.add(p);
                    }
                }
            }

            // Exécution du processus courant
            if (currentProcess != null && currentProcess.isActif()) {
                currentProcess.oneTimeSlice();
                quantumCounter++;

                if (currentProcess.getRemain_t() == 0) {
                    currentProcess.setFinished(true);
                    currentProcess.setActif(false);
                    currentProcess.setStateProcString("X");
                    currentProcess = null;
                    quantumCounter = 0;
                } else {
                    if ((currentProcess.getTotal_t() - currentProcess.getRemain_t()) == currentProcess.getIoAt_t() && currentProcess.getIoLastF_t() != 0) {
                        currentProcess.setActif(false);
                        currentProcess.setBlocked(true);
                        currentProcess.setStateProcString("B");
                        currentProcess = null;
                        quantumCounter = 0;
                    } else {
                        if (quantumCounter == maxSteps && !currentProcess.isFinished() && !currentProcess.isBlocked()) {
                            currentProcess.setActif(false);
                            currentProcess.setStateProcString("a");
                            readyQueue.add(currentProcess);
                            currentProcess = null;
                            quantumCounter = 0;
                        }
                    }
                }

            }

            // Sélection du processus si aucun en cours
            if (currentProcess == null) {
                if (!readyQueue.isEmpty()) {
                    currentProcess = readyQueue.poll();
                    currentProcess.setActif(true);
                    currentProcess.setStateProcString("A");
                    quantumCounter = 0;
                }
            }
            writeStateLine(processusTableau, ts, columnWidth, previousStates, previousBaseLine, outputFileName, ecran, 1, quantumCounter);
            String baseLine = buildBaseLine(processusTableau);
            previousBaseLine = baseLine;
            ts++;
        }

        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        File etatFichier = new File(outputFileName);
        String statesData = ecran.lireFile(etatFichier);
        System.out.println("\n=== Tableau des états (RR_IO) ===");
        System.out.print(statesData);

        return outputFileName;
    }

    private static String ordonnancementRoundRobinWithIOPriority(Processus[] processusTableau, IOCommandes ecran) {
        int nbProcessus = processusTableau.length;
        int startTime = 0;

        // Initialisation et configuration des processus
        if (nbProcessus > 0) {
            startTime = initializeAndPrepareProcesses(processusTableau);
        }

        // Affichage initial
        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        // Largeur de colonne
        int maxNameLength = 1;
        for (Processus p : processusTableau) {
            if (p.getNameProc().length() > maxNameLength) {
                maxNameLength = p.getNameProc().length();
            }
        }
        int stateLength = 8;
        int columnWidth = Math.max(maxNameLength, stateLength) + 7;

        // Préparer l'en-tête pour l'affichage
        StringBuilder header = new StringBuilder();
        header.append(String.format("%-" + columnWidth + "s", "X"));
        for (Processus p : processusTableau) {
            header.append(String.format("%-" + columnWidth + "s", p.getNameProc()));
        }

        String outputFileName = "etat_processus_rr_io_prio.txt";
        try {
            new File(outputFileName).delete();
            new File(outputFileName).createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ecran.ecrireFile(outputFileName, header.toString());

        int ts = startTime;
        int maxSteps = 10;
        Map<Integer, LinkedList<Processus>> priorityQueues = new TreeMap<>();

        for (Processus p : processusTableau) {
            if (p.isArrived() && !p.isFinished() && !p.isBlocked()) {
                priorityQueues.computeIfAbsent(p.getPriority_l(), k -> new LinkedList<>()).add(p);
            }
        }

        Processus currentProcess = null;
        int quantumCounter = 0;
        String previousBaseLine = null;
        String[] previousStates = new String[processusTableau.length];
        Arrays.fill(previousStates, "");

        while (!allProcessesFinished(processusTableau) && ts < startTime + 300) {
            // Arrivée de nouveaux processus
            for (Processus p : processusTableau) {
                if ((int) p.getArrive_t() == ts && !p.isArrived()) {
                    p.setArrived(true);
                    p.setStateProcString("a");
                    priorityQueues.computeIfAbsent(p.getPriority_l(), k -> new LinkedList<>()).add(p);
                }
            }

            // Fin d’I/O
            for (Processus p : processusTableau) {
                if (p.isBlocked()) {
                    p.oneBlockTimeSlice();
                    if (p.getIoLastF_t() == 0) {
                        p.setBlocked(false);
                        p.setStateProcString("a");
                        priorityQueues.computeIfAbsent(p.getPriority_l(), k -> new LinkedList<>()).add(p);
                    }
                }
            }

            // Exécution du processus courant
            if (currentProcess != null && currentProcess.isActif()) {
                currentProcess.oneTimeSlice();
                quantumCounter++;

                if (currentProcess.getRemain_t() == 0) {
                    currentProcess.setFinished(true);
                    currentProcess.setActif(false);
                    currentProcess.setStateProcString("X");
                    currentProcess = null;
                    quantumCounter = 0;
                } else {
                    if ((currentProcess.getTotal_t() - currentProcess.getRemain_t()) == currentProcess.getIoAt_t() && currentProcess.getIoLastF_t() != 0) {
                        currentProcess.setActif(false);
                        currentProcess.setBlocked(true);
                        currentProcess.setStateProcString("B");
                        currentProcess = null;
                        quantumCounter = 0;
                    } else {
                        if (quantumCounter == maxSteps && !currentProcess.isFinished() && !currentProcess.isBlocked()) {
                            currentProcess.setActif(false);
                            currentProcess.setStateProcString("a");
                            priorityQueues.get(currentProcess.getPriority_l()).add(currentProcess);
                            currentProcess = null;
                            quantumCounter = 0;
                        }
                    }
                }
            }

            // Sélection du processus courant si aucun n’est en cours
            if (currentProcess == null) {
                currentProcess = pickHighestPriorityProcess(priorityQueues);
                if (currentProcess != null) {
                    currentProcess.setActif(true);
                    currentProcess.setStateProcString("A");
                    quantumCounter = 0;
                }
            }



            writeStateLine(processusTableau, ts, columnWidth, previousStates, previousBaseLine, outputFileName, ecran, 1, quantumCounter);
            String baseLine = buildBaseLine(processusTableau);
            previousBaseLine = baseLine;
            ts++;
        }

        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        File etatFichier = new File(outputFileName);
        String statesData = ecran.lireFile(etatFichier);
        System.out.println("\n=== Tableau des états (RR_IO_PRIO) ===");
        System.out.print(statesData);

        return  outputFileName;
    }

    private static String buildStateLine(Processus[] processusTableau, int ts, int columnWidth, String[] previousStates) {
        StringBuilder fullLineBuilder = new StringBuilder();
        fullLineBuilder.append(String.format("%-" + columnWidth + "s", ts));
        for (int i = 0; i < processusTableau.length; i++) {
            Processus p = processusTableau[i];
            int executedTime = (int) (p.getTotal_t() - p.getRemain_t());
            String stateStr;
            if (p.isFinished() && previousStates[i].equals("A")) {
                stateStr = "A("  + executedTime + ")->" + p.getStateProcString();
            } else if (p.isFinished()) {
                stateStr = p.getStateProcString();
            /*}else if (p.getStateProcString().equals("A") && previousStates[i].equals("a")) {
                stateStr = "a->A(0)";
            }*/
            }else if (p.getStateProcString().equals("a") && previousStates[i].equals("A")) {
                stateStr = "A(" + executedTime + ")->a";
            } else if (p.isBlocked() && previousStates[i].equals("A")) {
                stateStr = "A(" + executedTime + ")->B(" + (int)p.getIoLastF_t() + ")";
            } else if (p.isBlocked()) {
                stateStr = "B(" + (int)p.getIoLastF_t() + ")";
            }else if (p.getStateProcString().equals("a") && previousStates[i].equals("B")) {
                stateStr = "B->a(" + executedTime + ")";
            }else if (p.getStateProcString().equals("A") && previousStates[i].equals("B")) {
                stateStr = "B->A(" + executedTime + ")";
            }else if (p.getStateProcString().equals("A") && previousStates[i].equals("a")) {
                stateStr = "a->A(" + executedTime + ")";
            } else if (p.getStateProcString().equals("a")) {
                stateStr = "a";
            } else if (p.isArrived()) {
                stateStr = p.getStateProcString() + "(" + executedTime + ")";
            }else {
                stateStr = "_";
            }

            previousStates[i] = p.getStateProcString();
            fullLineBuilder.append(String.format("%-" + columnWidth + "s", stateStr));
        }
        return fullLineBuilder.toString();
    }

    private static int writeStateLine(Processus[] processusTableau, int ts, int columnWidth, String[] previousStates,
                                      String previousBaseLine, String outputFileName, IOCommandes ecran, int usingOrdoType, int pas) {
        String baseLine = buildBaseLine(processusTableau);
        String fullLine = buildStateLine(processusTableau, ts, columnWidth, previousStates);

        switch (usingOrdoType) {
            case 1:
                // On écrit si changement d'état ou pas == 10
                if (!baseLine.equals(previousBaseLine) || pas == 10) {
                    ecran.ecrireFile(outputFileName, fullLine);
                    // Si on a atteint 10, on réinitialise pas
                    if (pas == 10) {
                        pas = 0;
                    }
                }
                break;

            default:
                // Écriture si changement d'état ou ts multiple de 10
                if (!baseLine.equals(previousBaseLine) || ts % 10 == 0) {
                    ecran.ecrireFile(outputFileName, fullLine);
                }
                break;
        }

        return pas;
    }


    private static boolean allProcessesFinished(Processus[] processusTableau) {
        for (Processus p : processusTableau) {
            if (!p.isFinished()) {
                return false;
            }
        }
        return true;
    }
}
