import java.io.File;
import java.util.*;

class Dispatcher {

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
        Scanner scanner = new Scanner(System.in);
        IOCommandes ecran = new IOCommandes();

        // Étape 1 : Sélectionner un fichier parmi les trois fichiers disponibles
        System.out.println("Veuillez sélectionner un fichier parmi les suivants :");
        System.out.println("1. processus_1.txt");
        System.out.println("2. processus_2.txt");
        System.out.println("3. processus_TD.txt");

        System.out.print("Entrez le numéro de votre choix : ");
        int fileChoice = scanner.nextInt();
        scanner.nextLine(); // Consommer la nouvelle ligne

        File selectedFile;
        switch (fileChoice) {
            case 1:
                selectedFile = new File("./files/processus_1.txt");
                break;
            case 2:
                selectedFile = new File("./files/processus_2.txt");
                break;
            case 3:
                selectedFile = new File("./files/processus_TD.txt");
                break;
            default:
                System.out.println("Choix invalide. Fin du programme.");
                return;
        }

        String data = ecran.lireFile(selectedFile);
        ecran.afficheProcess(data);
        Processus[] processusTableau = ecran.tableProcess(data);

        // Étape 2 : Sélectionner un type d'ordonnancement
        System.out.println("Veuillez sélectionner un type d'ordonnancement :");
        System.out.println("1. Ordonnancement FIFO sans I/O sans priorité (basique)");
        System.out.println("2. Ordonnancement FIFO par priorité avec I/O préemption");
        System.out.println("3. Ordonnancement Round-Robin avec I/O sans priorité");
        System.out.println("4. Ordonnancement Round-Robin avec I/O avec priorité préemption");

        System.out.print("Entrez le numéro de votre choix : ");
        int schedulingChoice = scanner.nextInt();
        scanner.nextLine(); // Consommer la nouvelle ligne

        switch (schedulingChoice) {
            case 1:
                ordonnancementFIFOWithoutIONoPriority(processusTableau, ecran);
                break;
            case 2:
                ordonnancementIOPriority(processusTableau, ecran);
                break;
            case 3:
                ordonnancementRoundRobinWithIONoPriority(processusTableau, ecran);
                break;
            case 4:
                ordonnancementRoundRobinWithIOPriority(processusTableau, ecran);
                break;
            default:
                System.out.println("Choix d'ordonnancement invalide. Fin du programme.");
        }
    }

    private static void ordonnancementFIFOWithoutIONoPriority(Processus[] processusTableau, IOCommandes ecran) {
        int nbProcessus = processusTableau.length;

        // Initialisation
        int startTime = 0;
        if (nbProcessus > 0) {
            float minArrive = Float.MAX_VALUE;
            for (Processus p : processusTableau) {
                if (p.getArrive_t() < minArrive) {
                    minArrive = p.getArrive_t();
                }
            }

            List<Processus> candidats = new ArrayList<>();
            for (Processus p : processusTableau) {
                if (p.getArrive_t() == minArrive) {
                    candidats.add(p);
                }
            }

            // Le premier arrivé
            Processus premier = candidats.get(0);
            premier.setActif(false);
            premier.setStateProcString("a");
            minArrive = premier.getArrive_t();
            premier.setArrived(true);
            startTime = (int) minArrive;
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
        int columnWidth = Math.max(maxNameLength, stateLength) + 2;

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

            previousBaseLine = writeStateLine(processusTableau, ts, columnWidth, previousStates, previousBaseLine, outputFileName, ecran);

            ts++;
            if (actif != null && !actif.isFinished()) {
                actif.oneTimeSlice();
            }
        }

        File etatFichier = new File(outputFileName);
        String statesData = ecran.lireFile(etatFichier);
        System.out.println("\n=== Tableau des états (relu depuis le fichier) ===");
        System.out.print(statesData);

        System.out.println("\nOrdonnancement FIFO terminé.");
    }

    private static void ordonnancementIOPriority(Processus[] processusTableau, IOCommandes ecran) {
        int nbProcessus = processusTableau.length;

        // Initialisation
        int startTime = 0;
        if (nbProcessus > 0) {
            float minArrive = Float.MAX_VALUE;
            for (Processus p : processusTableau) {
                if (p.getArrive_t() < minArrive) {
                    minArrive = p.getArrive_t();
                }
            }

            List<Processus> candidats = new ArrayList<>();
            for (Processus p : processusTableau) {
                if (p.getArrive_t() == minArrive) {
                    candidats.add(p);
                }
            }

            Processus plusImportant = null;
            int plusFaiblePriorite = Integer.MAX_VALUE;
            for (Processus p : candidats) {
                if (p.getPriority_l() < plusFaiblePriorite) {
                    plusFaiblePriorite = p.getPriority_l();
                    plusImportant = p;
                }
            }

            if (plusImportant != null) {
                plusImportant.setActif(false);
                plusImportant.setStateProcString("a");
                minArrive = plusImportant.getArrive_t();
                plusImportant.setArrived(true);
                for (Processus p : candidats) {
                    if (p != plusImportant && p.getArrive_t() == plusImportant.getArrive_t()) {
                        p.setArrived(true);
                        p.setStateProcString("a");
                    }
                }
            }
            startTime = (int) minArrive;
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
        int columnWidth = Math.max(maxNameLength, stateLength) + 2;

        StringBuilder header = new StringBuilder();
        header.append(String.format("%-" + columnWidth + "s", "X"));
        for (Processus p : processusTableau) {
            header.append(String.format("%-" + columnWidth + "s", p.getNameProc()));
        }

        String outputFileName = "etat_processus.txt";
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
            for (Processus p : processusTableau) {
                if (p.isActif() && p.getRemain_t() > 0) {
                    p.oneTimeSlice();
                    if (p.getRemain_t() == 0) {
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

                if (!p.isFinished() && !p.isBlocked()
                        && (p.getTotal_t() - p.getRemain_t()) == p.getIoAt_t()
                        && p.getIoLastF_t() != 0) {
                    p.setActif(false);
                    p.setBlocked(true);
                    p.setStateProcString("B");
                }

                if (p.getArrive_t() == ts && !p.isArrived()) {
                    p.setArrived(true);
                    if (!p.isFinished() && !p.isBlocked() && !p.isActif()) {
                        p.setStateProcString("a");
                    }
                }
            }

            // Choix du plus important
            Processus plusImportant2 = null;
            int plusFaiblePriorite = Integer.MAX_VALUE;
            for (Processus p : processusTableau) {
                if (!p.isFinished() && !p.isBlocked() && p.isArrived()) {
                    if (p.getPriority_l() < plusFaiblePriorite) {
                        plusFaiblePriorite = p.getPriority_l();
                        plusImportant2 = p;
                    }
                }
            }

            if (plusImportant2 != null) {
                for (Processus p : processusTableau) {
                    if (p != plusImportant2 && p.isActif()) {
                        p.setActif(false);
                        if (!p.isFinished() && !p.isBlocked()) {
                            p.setStateProcString("a");
                        }
                    }
                }
                plusImportant2.setActif(true);
                plusImportant2.setStateProcString("A");
                plusImportant2.setArrived(true);
            }

            previousBaseLine = writeStateLine(processusTableau, ts, columnWidth, previousStates, previousBaseLine, outputFileName, ecran);
            ts++;
        }

        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        File etatFichier = new File(outputFileName);
        String statesData = ecran.lireFile(etatFichier);
        System.out.println("\n=== Tableau des états (relu depuis le fichier) ===");
        System.out.print(statesData);

        System.out.println("\nOrdonnancement par priorité terminé.");
    }

    private static void ordonnancementRoundRobinWithIONoPriority(Processus[] processusTableau, IOCommandes ecran) {
        int nbProcessus = processusTableau.length;

        // Initialisation
        int startTime = 0;
        if (nbProcessus > 0) {
            float minArrive = Float.MAX_VALUE;
            for (Processus p : processusTableau) {
                if (p.getArrive_t() < minArrive) {
                    minArrive = p.getArrive_t();
                }
            }

            List<Processus> candidats = new ArrayList<>();
            for (Processus p : processusTableau) {
                if (p.getArrive_t() == minArrive) {
                    candidats.add(p);
                }
            }

            Processus plusImportant = null;
            int plusFaiblePriorite = Integer.MAX_VALUE;
            for (Processus p : candidats) {
                if (p.getPriority_l() < plusFaiblePriorite) {
                    plusFaiblePriorite = p.getPriority_l();
                    plusImportant = p;
                }
            }

            if (plusImportant != null) {
                plusImportant.setActif(false);
                plusImportant.setStateProcString("a");
                minArrive = plusImportant.getArrive_t();
                // Les autres arrivés en même temps
                for (Processus p : candidats) {
                    if (p != plusImportant && p.getArrive_t() == plusImportant.getArrive_t()) {
                        p.setStateProcString("a");
                    }
                }
            }
            startTime = (int) minArrive;

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
        int columnWidth = Math.max(maxNameLength, stateLength) + 2;

        StringBuilder header = new StringBuilder();
        header.append(String.format("%-" + columnWidth + "s", "X"));
        for (Processus p : processusTableau) {
            header.append(String.format("%-" + columnWidth + "s", p.getNameProc()));
        }

        String outputFileName = "etat_processus.txt";
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

            previousBaseLine = writeStateLine(processusTableau, ts, columnWidth, previousStates, previousBaseLine, outputFileName, ecran);
            ts++;
        }

        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        File etatFichier = new File(outputFileName);
        String statesData = ecran.lireFile(etatFichier);
        System.out.println("\n=== Tableau des états (relu depuis le fichier) ===");
        System.out.print(statesData);
    }

    private static void ordonnancementRoundRobinWithIOPriority(Processus[] processusTableau, IOCommandes ecran) {
        int nbProcessus = processusTableau.length;

        // Initialisation
        int startTime = 0;
        if (nbProcessus > 0) {
            float minArrive = Float.MAX_VALUE;
            for (Processus p : processusTableau) {
                if (p.getArrive_t() < minArrive) {
                    minArrive = p.getArrive_t();
                }
            }

            List<Processus> candidats = new ArrayList<>();
            for (Processus p : processusTableau) {
                if (p.getArrive_t() == minArrive) {
                    candidats.add(p);
                }
            }

            Processus plusImportant = null;
            int plusFaiblePriorite = Integer.MAX_VALUE;
            for (Processus p : candidats) {
                if (p.getPriority_l() < plusFaiblePriorite) {
                    plusFaiblePriorite = p.getPriority_l();
                    plusImportant = p;
                }
            }

            if (plusImportant != null) {
                plusImportant.setActif(false);
                plusImportant.setStateProcString("a");
                minArrive = plusImportant.getArrive_t();
                for (Processus p : candidats) {
                    if (p != plusImportant && p.getArrive_t() == plusImportant.getArrive_t()) {
                        p.setStateProcString("a");
                    }
                }
            }
            startTime = (int) minArrive;
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
        int columnWidth = Math.max(maxNameLength, stateLength) + 2;

        StringBuilder header = new StringBuilder();
        header.append(String.format("%-" + columnWidth + "s", "X"));
        for (Processus p : processusTableau) {
            header.append(String.format("%-" + columnWidth + "s", p.getNameProc()));
        }

        String outputFileName = "etat_processus.txt";
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

            previousBaseLine = writeStateLine(processusTableau, ts, columnWidth, previousStates, previousBaseLine, outputFileName, ecran);
            ts++;
        }

        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        File etatFichier = new File(outputFileName);
        String statesData = ecran.lireFile(etatFichier);
        System.out.println("\n=== Tableau des états (relu depuis le fichier) ===");
        System.out.print(statesData);
    }

    private static String buildStateLine(Processus[] processusTableau, int ts, int columnWidth, String[] previousStates) {
        StringBuilder fullLineBuilder = new StringBuilder();
        fullLineBuilder.append(String.format("%-" + columnWidth + "s", ts));
        for (int i = 0; i < processusTableau.length; i++) {
            Processus p = processusTableau[i];
            String stateStr;
            if (p.isFinished()) {
                stateStr = p.getStateProcString();
            } else if (p.getStateProcString().equals("A") && previousStates[i].equals("a")) {
                stateStr = "a->A(0)";
            } else if (p.getStateProcString().equals("a")) {
                stateStr = "a";
            } else if (p.isBlocked() && previousStates[i].equals("A")) {
                stateStr = "A->B(0)";
            } else {
                int executedTime = (int) (p.getTotal_t() - p.getRemain_t());
                stateStr = p.getStateProcString() + "(" + executedTime + ")";
            }

            previousStates[i] = p.getStateProcString();
            fullLineBuilder.append(String.format("%-" + columnWidth + "s", stateStr));
        }
        return fullLineBuilder.toString();
    }

    private static String writeStateLine(Processus[] processusTableau, int ts, int columnWidth, String[] previousStates,
                                         String previousBaseLine, String outputFileName, IOCommandes ecran) {
        String baseLine = buildBaseLine(processusTableau);
        String fullLine = buildStateLine(processusTableau, ts, columnWidth, previousStates);

        if (!baseLine.equals(previousBaseLine) || ts % 10 == 0) {
            ecran.ecrireFile(outputFileName, fullLine);
            return baseLine;
        }
        return baseLine;
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
