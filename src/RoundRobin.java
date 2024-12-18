import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class RoundRobin {
    public static void main(String[] args) {
        IOCommandes ecran = new IOCommandes();
        File fichier = new File("files/processus_TD.txt");
        String data = ecran.lireFile(fichier);
        ecran.afficheProcess(data);

        // Transformation du contenu en tableau de Processus
        Processus[] processusTableau = ecran.tableProcess(data);
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

        String previousBaseLine = null; // Conserver la baseLine précédente

        int ts = startTime;
        boolean preemptive = true;
        String algorithm = "RR"; // "PRIORITY" ou "RR"
        int quantum = 5;
        int quantumCounter = 0;
        Queue<Processus> readyQueue = new LinkedList<>();

// Initialisation: ajouter les processus arrivés au début dans la file
        for (Processus p : processusTableau) {
            if (p.isArrived()) {
                readyQueue.add(p);
            }
        }

        Processus currentProcess = null;

        while (!allProcessesFinished(processusTableau) && ts < startTime + 300) {

            // Gérer l'arrivée de nouveaux processus
            for (Processus p : processusTableau) {
                if (p.getArrive_t() == ts && !p.isArrived()) {
                    p.setArrived(true);
                    p.setStateProcString("a");
                    readyQueue.add(p);
                }
            }

            // Gérer la fin d’I/O
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

            // Sélection du processus courant si aucun n’est en cours
            if (currentProcess == null) {
                if (!readyQueue.isEmpty()) {
                    currentProcess = readyQueue.poll();
                    currentProcess.setActif(true);
                    currentProcess.setStateProcString("A");
                    quantumCounter = 0; // réinitialiser le quantum pour ce processus
                }
            }

            // Exécution du processus courant
            if (currentProcess != null && currentProcess.isActif()) {
                // Exécuter une unité de temps
                currentProcess.oneTimeSlice();
                quantumCounter++;

                // Si le processus est terminé
                if (currentProcess.getRemain_t() == 0) {
                    currentProcess.setFinished(true);
                    currentProcess.setActif(false);
                    currentProcess.setStateProcString("X");
                    currentProcess = null; // On reprendront un autre à la prochaine itération
                } else {
                    // Si c'est le temps d’I/O
                    if ((currentProcess.getTotal_t() - currentProcess.getRemain_t()) == currentProcess.getIoAt_t() && currentProcess.getIoLastF_t() != 0) {
                        currentProcess.setActif(false);
                        currentProcess.setBlocked(true);
                        currentProcess.setStateProcString("B");
                        currentProcess = null;
                    } else {
                        // Vérifier le quantum si RR préemptif
                        if (algorithm.equals("RR") && preemptive) {
                            if (quantumCounter == quantum && !currentProcess.isFinished() && !currentProcess.isBlocked()) {
                                // Quantum expiré, réinsérer à la fin de la file
                                currentProcess.setActif(false);
                                currentProcess.setStateProcString("a");
                                readyQueue.add(currentProcess);
                                currentProcess = null;
                            }
                        }
                    }
                }
            }

            // Construction de baseLine (sans temps)
            StringBuilder baseLineBuilder = new StringBuilder();
            for (Processus p : processusTableau) {
                String stateStr = p.getStateProcString();
                baseLineBuilder.append(stateStr);
                baseLineBuilder.append(" "); // un espace pour séparer
            }
            String baseLine = baseLineBuilder.toString();

// Construction de la fullLine (avec ts et temps d'exécution)
            String fullLine = buildStateLine(processusTableau, ts, columnWidth);

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

    /**
     * Construit la ligne d'état formatée pour un temps donné ts.
     * @param processusTableau tableau des processus
     * @param ts le temps courant
     * @param columnWidth la largeur de chaque colonne
     * @return une ligne formatée
     */
    private static String buildStateLine(Processus[] processusTableau, int ts, int columnWidth) {
        StringBuilder fullLineBuilder = new StringBuilder();
        fullLineBuilder.append(String.format("%-" + columnWidth + "s", String.valueOf(ts)));
        for (Processus p : processusTableau) {
            int executedTime = (int) (p.getTotal_t() - p.getRemain_t());
            String stateStr = p.getStateProcString() + "(" + executedTime + ")";
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
