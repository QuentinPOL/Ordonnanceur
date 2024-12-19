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
        scanner.nextLine(); // Consommer la nouvelle ligne restante

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
        System.out.println("1. Ordonnancement FIFO sans I/O sans priorité (basique)"); // Fini (a implementer
        System.out.println("2. Ordonnancement FIFO par priorité avec I/O préemption"); // Fini
        System.out.println("3. Ordonnancement Round-Robin avec I/O sans priorité"); // Fini (a implementer)
        System.out.println("4. Ordonnancement Round-Robin avec I/O avec priorité préemption"); // Manque lui

        System.out.print("Entrez le numéro de votre choix : ");
        int schedulingChoice = scanner.nextInt();
        scanner.nextLine(); // Consommer la nouvelle ligne restante

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

        // Initialisation : choisir le processus avec l'heure d'arrivée minimale
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

            Processus premier = candidats.get(0); // Le premier arrivé
            premier.setActif(false);
            premier.setStateProcString("a");
            minArrive = premier.getArrive_t();
            premier.setArrived(true);
            startTime = (int) minArrive;
        }

        // Affichage des informations initiales
        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        // Détermination de la largeur de colonne pour un joli tableau
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
            String oldLine = buildStateLine(processusTableau, ts, columnWidth, previousStates);

            // Activer les processus arrivés à ce temps
            for (Processus p : processusTableau) {
                if (p.getArrive_t() == ts && !p.isArrived()) {
                    p.setArrived(true);
                    if (!p.isFinished()) {
                        p.setStateProcString("a");
                    }
                }
            }

            // Trouver un processus actif ou en démarrer un si nécessaire
            Processus actif = null;
            for (Processus p : processusTableau) {
                if (p.isActif()) {
                    actif = p;
                    break;
                }
            }

            // Si aucun processus n'est actif, chercher le premier processus prêt
            if (actif == null) {
                for (Processus p : processusTableau) {
                    if (!p.isFinished() && p.isArrived()) {
                        actif = p;
                        break;
                    }
                }

                if (actif != null) {
                    actif.setActif(true);
                    actif.setStateProcString("A"); // Initialisation correcte avec le temps exécuté
                }
            }

            // Exécuter le processus actif
            if (actif != null) {
                int executedTime = (int) (actif.getTotal_t() - actif.getRemain_t());

                if (actif.getRemain_t() == 0) {
                    actif.setActif(false);
                    actif.setFinished(true);
                    actif.setStateProcString("X");

                    // Recherche immédiate d'un autre processus prêt à être exécuté
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
                        continue; // Recommencer la boucle pour exécuter immédiatement le processus suivant
                    }
                }
            }

            String fullLine = buildStateLine(processusTableau, ts, columnWidth, previousStates);

            if (previousBaseLine == null || !fullLine.equals(previousBaseLine) || ts % 10 == 0) {
                ecran.ecrireFile(outputFileName, fullLine);
                previousBaseLine = fullLine;
            }


            ts++;

            actif.oneTimeSlice();
        }

        File etatFichier = new File(outputFileName);
        String statesData = ecran.lireFile(etatFichier);
        System.out.println("\n=== Tableau des états (relu depuis le fichier) ===");
        System.out.print(statesData);

        System.out.println("\nOrdonnancement FIFO terminé.");
    }

    private static void ordonnancementIOPriority(Processus[] processusTableau, IOCommandes ecran) {
        int nbProcessus = processusTableau.length;

        // Initialisation : choisir le processus prioritaire à l'heure d'arrivée minimale
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

        // Affichage des informations initiales
        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        // Détermination de la largeur de colonne pour un joli tableau
        int maxNameLength = 1;
        for (Processus p : processusTableau) {
            if (p.getNameProc().length() > maxNameLength) {
                maxNameLength = p.getNameProc().length();
            }
        }

        // Les états seront du type A(1234), max ~ 7-8 caractères
        int stateLength = 8;
        int columnWidth = Math.max(maxNameLength, stateLength) + 2; // 2 espaces de marge

        // Préparation de l'en-tête du tableau : "X" + noms des processus
        StringBuilder header = new StringBuilder();

        header.append(String.format("%-" + columnWidth + "s", "X"));
        for (Processus p : processusTableau) {
            header.append(String.format("%-" + columnWidth + "s", p.getNameProc()));
        }

        String outputFileName = "etat_processus.txt";

        // Réinitialisation du fichier
        try {
            new File(outputFileName).delete();
            new File(outputFileName).createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Ecriture de l'en-tête dans le fichier
        ecran.ecrireFile(outputFileName, header.toString());

        String previousBaseLine = null; // Conserver la baseLine précédente
        String[] previousStates = new String[processusTableau.length];
        Arrays.fill(previousStates, ""); // États initiaux vides

        int ts = startTime;
        while (!allProcessesFinished(processusTableau) && ts < startTime + 300) {
            // Sauvegarder l'ancien état avant mise à jour
            String oldLine = buildStateLine(processusTableau, ts, columnWidth, previousStates);

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

                if (!p.isFinished() && !p.isBlocked() &&
                        (p.getTotal_t() - p.getRemain_t()) == p.getIoAt_t() && p.getIoLastF_t() != 0) {
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

            String fullLine = buildStateLine(processusTableau, ts, columnWidth, previousStates);

            if (previousBaseLine == null || !fullLine.equals(previousBaseLine) || ts % 10 == 0) {
                ecran.ecrireFile(outputFileName, fullLine);
                previousBaseLine = fullLine;
            }
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

        // Initialisation : choisir le processus prioritaire à l'heure d'arrivée minimale

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
                //plusImportant.setArrived(true);
                for (Processus p : candidats) {
                    if (p != plusImportant && p.getArrive_t() == plusImportant.getArrive_t()) {
                        //p.setArrived(true);
                        p.setStateProcString("a");
                    }
                }
            }
            startTime = (int) minArrive;

        }

        // Affichage des informations initiales
        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        // Détermination de la largeur de colonne pour un joli tableau
        int maxNameLength = 1;
        for (Processus p : processusTableau) {
            if (p.getNameProc().length() > maxNameLength) {
                maxNameLength = p.getNameProc().length();
            }
        }
        // Les états seront du type A(1234), max ~ 7-8 caractères
        int stateLength = 8;
        int columnWidth = Math.max(maxNameLength, stateLength) + 2; // 2 espaces de marge

        // Préparation de l'en-tête du tableau : "X" + noms des processus
        StringBuilder header = new StringBuilder();
        // La première colonne sera "X" (pour le temps)
        header.append(String.format("%-" + columnWidth + "s", "X"));
        for (Processus p : processusTableau) {
            header.append(String.format("%-" + columnWidth + "s", p.getNameProc()));
        }

        String outputFileName = "etat_processus.txt";

        // Réinitialisation du fichier
        try {
            new File(outputFileName).delete();
            new File(outputFileName).createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Ecriture de l'en-tête dans le fichier
        ecran.ecrireFile(outputFileName, header.toString());

        //String previousBaseLine = null; // Conserver la baseLine précédente

        int ts = startTime;
        boolean preemptive = true;
        String algorithm = "RR"; // On reste en RR
        int maxSteps = 10; // Le processus doit rester 10 pas d'affilée
        int quantumCounter = 0; // Compteur de pas pour le processus courant
        LinkedList<Processus> readyQueue = new LinkedList<>();

        // Initialisation: ajouter les processus déjà arrivés au début
        for (Processus p : processusTableau) {
            if (p.isArrived() && !p.isFinished() && !p.isBlocked()) {
                readyQueue.add(p);
            }
        }

        Processus currentProcess = null;

        while (!allProcessesFinished(processusTableau) && ts < startTime + 300) {
            boolean arrivalHappened = false;
            if (ts==225)
                arrivalHappened = false;
            // Gérer l'arrivée de nouveaux processus
            for (Processus p : processusTableau) {
                if ((int)p.getArrive_t() == ts && !p.isArrived()) {
                    p.setArrived(true);
                    p.setStateProcString("a");
                    readyQueue.add(p);
                    arrivalHappened = true;
                }
            }

            // Gérer la fin d’I/O
            for (Processus p : processusTableau) {
                if (p.isBlocked()) {
                    p.oneBlockTimeSlice();
                    if (p.getIoLastF_t() == 0) {
                        p.setBlocked(false);
                        p.setStateProcString("a");
                        readyQueue.add(p); // Ajouter à la fin

                        arrivalHappened = true; // Un processus vient de redevenir disponible
                    }
                }
            }

            // Exécution du processus courant
            if (currentProcess != null && currentProcess.isActif()) {
                // Exécuter une unité de temps (un pas)
                currentProcess.oneTimeSlice();
                quantumCounter++;

                // Si le processus est terminé
                if (currentProcess.getRemain_t() == 0) {
                    currentProcess.setFinished(true);
                    currentProcess.setActif(false);
                    currentProcess.setStateProcString("X");
                    currentProcess = null;
                    quantumCounter = 0;
                } else {
                    // Si c'est le temps d’I/O
                    if ((currentProcess.getTotal_t() - currentProcess.getRemain_t()) == currentProcess.getIoAt_t() && currentProcess.getIoLastF_t() != 0) {
                        currentProcess.setActif(false);
                        currentProcess.setBlocked(true);
                        currentProcess.setStateProcString("B");
                        currentProcess = null;
                        quantumCounter = 0;
                    } else {
                        // Si le quantum de pas est atteint
                        if (quantumCounter == maxSteps && !currentProcess.isFinished() && !currentProcess.isBlocked()) {
                            // Quantum expiré, réinsérer à la fin de la file
                            currentProcess.setActif(false);
                            currentProcess.setStateProcString("a");
                            readyQueue.add(currentProcess); // Remettre à la fin de la file
                            currentProcess = null;
                            quantumCounter = 0;
                        }
                    }
                }

            }

            // Si un processus est en cours, mais qu'un nouvel arrivant est arrivé, on préempte immédiatement
            // (si preemptive = true et algorithm = "RR")
            /*
            if (currentProcess != null && arrivalHappened && preemptive && algorithm.equals("RR")) {
                // Préemption immédiate
                currentProcess.setActif(false);
                if (!currentProcess.isFinished() && !currentProcess.isBlocked()) {
                    currentProcess.setStateProcString("a");
                    readyQueue.add(currentProcess);
                }
                currentProcess = null;
                quantumCounter = 0;
            }*/

            // Sélection du processus courant si aucun n’est en cours

            if (currentProcess == null) {
                if (!readyQueue.isEmpty()) {
                    currentProcess = readyQueue.poll();
                    currentProcess.setActif(true);
                    currentProcess.setStateProcString("A");
                    quantumCounter = 0; // réinitialiser le quantum pour ce processus
                }
            }

            String previousBaseLine = null; // Conserver la baseLine précédente
            String[] previousStates = new String[processusTableau.length];
            Arrays.fill(previousStates, ""); // États initiaux vides

            // Construction de baseLine (sans temps)
            StringBuilder baseLineBuilder = new StringBuilder();
            for (Processus p : processusTableau) {
                String stateStr = p.getStateProcString();
                baseLineBuilder.append(stateStr);
                baseLineBuilder.append(" ");
            }
            String baseLine = baseLineBuilder.toString();

            // Construction de la fullLine (avec ts et temps d'exécution)
            String fullLine = buildStateLine(processusTableau, ts, columnWidth, previousStates);

            // Comparaison des baseLines pour détecter les changements
            if (previousBaseLine == null || !baseLine.equals(previousBaseLine) || ts%10==0) {
                ecran.ecrireFile(outputFileName, fullLine);
                previousBaseLine = baseLine;
            }

            ts++;
        }

        // Affichage final des objets
        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        // Maintenant, on relit le fichier etat_processus.txt et on l'affiche sous forme de tableau
        File etatFichier = new File(outputFileName);
        String statesData = ecran.lireFile(etatFichier);
        System.out.println("\n=== Tableau des états (relu depuis le fichier) ===");
        System.out.print(statesData);

    }

    private static void ordonnancementRoundRobinWithIOPriority(Processus[] processusTableau, IOCommandes ecran) {
        int nbProcessus = processusTableau.length;

        // Initialisation : choisir le processus prioritaire à l'heure d'arrivée minimale
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
                // Les autres sont arrivés mais en 'a'
                for (Processus p : candidats) {
                    if (p != plusImportant && p.getArrive_t() == plusImportant.getArrive_t()) {
                        p.setStateProcString("a");
                    }
                }
            }
            startTime = (int) minArrive;
        }

        // Affichage des informations initiales
        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        // Détermination de la largeur de colonne pour un joli tableau
        int maxNameLength = 1;
        for (Processus p : processusTableau) {
            if (p.getNameProc().length() > maxNameLength) {
                maxNameLength = p.getNameProc().length();
            }
        }
        // Les états seront du type A(1234), max ~ 7-8 caractères
        int stateLength = 8;
        int columnWidth = Math.max(maxNameLength, stateLength) + 2; // 2 espaces de marge

        // Préparation de l'en-tête du tableau : "X" + noms des processus
        StringBuilder header = new StringBuilder();
        // La première colonne sera "X" (pour le temps)
        header.append(String.format("%-" + columnWidth + "s", "X"));
        for (Processus p : processusTableau) {
            header.append(String.format("%-" + columnWidth + "s", p.getNameProc()));
        }

        String outputFileName = "etat_processus.txt";

        // Réinitialisation du fichier
        try {
            new File(outputFileName).delete();
            new File(outputFileName).createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Ecriture de l'en-tête dans le fichier
        ecran.ecrireFile(outputFileName, header.toString());

        int ts = startTime;
        int maxSteps = 10; // Le quantum (nombre de time slice avant réinsertion)

        // Map triée par clé (priorité), la plus petite clé = plus haute priorité
        // Chaque priorité a sa propre file.
        Map<Integer, LinkedList<Processus>> priorityQueues = new TreeMap<>();

        // Initialisation: ajouter les processus déjà arrivés
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
            boolean arrivalHappened = false;

            // Arrivée de nouveaux processus à l'instant ts
            for (Processus p : processusTableau) {
                if ((int) p.getArrive_t() == ts && !p.isArrived()) {
                    p.setArrived(true);
                    p.setStateProcString("a");
                    priorityQueues.computeIfAbsent(p.getPriority_l(), k -> new LinkedList<>()).add(p);
                    arrivalHappened = true;
                }
            }

            // Fin d'I/O
            for (Processus p : processusTableau) {
                if (p.isBlocked()) {
                    p.oneBlockTimeSlice();
                    if (p.getIoLastF_t() == 0) {
                        p.setBlocked(false);
                        p.setStateProcString("a");
                        priorityQueues.computeIfAbsent(p.getPriority_l(), k -> new LinkedList<>()).add(p);
                        arrivalHappened = true;
                    }
                }
            }

            // Exécution du processus courant
            if (currentProcess != null && currentProcess.isActif()) {
                currentProcess.oneTimeSlice();
                quantumCounter++;

                // Si le processus est terminé
                if (currentProcess.getRemain_t() == 0) {
                    currentProcess.setFinished(true);
                    currentProcess.setActif(false);
                    currentProcess.setStateProcString("X");
                    currentProcess = null;
                    quantumCounter = 0;
                } else {
                    // Si c'est le temps d’I/O
                    if ((currentProcess.getTotal_t() - currentProcess.getRemain_t()) == currentProcess.getIoAt_t() && currentProcess.getIoLastF_t() != 0) {
                        currentProcess.setActif(false);
                        currentProcess.setBlocked(true);
                        currentProcess.setStateProcString("B");
                        currentProcess = null;
                        quantumCounter = 0;
                    } else {
                        // Si le quantum de pas est atteint
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


            // Construction de la ligne d'état
            String baseLine = buildBaseLine(processusTableau);
            String fullLine = buildStateLine(processusTableau, ts, columnWidth, previousStates);

            // On écrit si changement d'état ou toutes les 10 unités
            if (previousBaseLine == null || !baseLine.equals(previousBaseLine) || ts % 10 == 0) {
                ecran.ecrireFile(outputFileName, fullLine);
                previousBaseLine = baseLine;
            }

            ts++;
        }

        // Affichage final des objets
        for (Processus p : processusTableau) {
            System.out.println(p.toString());
        }

        // Maintenant, on relit le fichier etat_processus.txt
        File etatFichier = new File(outputFileName);
        String statesData = ecran.lireFile(etatFichier);
        System.out.println("\n=== Tableau des états (relu depuis le fichier) ===");
        System.out.print(statesData);
    }

    private static String buildStateLine(Processus[] processusTableau, int ts, int columnWidth, String[] previousStates) {
        StringBuilder fullLineBuilder = new StringBuilder();
        fullLineBuilder.append(String.format("%-" + columnWidth + "s", String.valueOf(ts)));
        for (int i = 0; i < processusTableau.length; i++) {
            Processus p = processusTableau[i];
            String stateStr;
            if (p.isFinished()) {
                stateStr = p.getStateProcString(); // "X" pour les processus terminés
            } else if (p.getStateProcString().equals("A") && previousStates[i].equals("a")) {
                stateStr = "a->A(0)"; // Transition de a à A(0)
            } else if (p.getStateProcString().equals("a")) {
                stateStr = "a"; // Toujours afficher "a" seul
            } else if (p.isBlocked() && previousStates[i].equals("A")) {
                stateStr = "A->B(0)"; // Transition vers B(0)
            } else {
                int executedTime = (int) (p.getTotal_t() - p.getRemain_t());
                stateStr = p.getStateProcString() + "(" + executedTime + ")"; // État + temps exécuté
            }

            // Mettre à jour l'état précédent
            previousStates[i] = p.getStateProcString();

            fullLineBuilder.append(String.format("%-" + columnWidth + "s", stateStr));
        }
        return fullLineBuilder.toString();
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