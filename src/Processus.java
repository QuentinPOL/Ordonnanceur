//Nomprocessus	DateArrive	DureeExecution	DateDebutIO	DureeIO	Priorite

class Processus {
	// Attributs d'origine pour la réinitialisation
	private final float originalArrive_t;
	private final float originalRemain_t;
	private final float originalIoAt_t;
	private final float originalIoLastF_t;
	private final int originalPriority_l;


	// Attributs
	private String nameProc;

	private String stateProcString;

	private float arrive_t, remain_t, total_t, ioAt_t, ioLastF_t;
	private int priority_l;

	private boolean finished;


	private boolean actif;


	private boolean arrived;

	private boolean block;
	public void oneTimeSlice(){
		this.remain_t-=1;
	}
	public void oneBlockTimeSlice(){
		this.ioLastF_t-=1;
	}

	public void setActif(boolean actif) {
		this.actif = actif;
	}
	public void setBlocked(boolean actif) {
		this.block = actif;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}
	public void setArrived(boolean arrived) {
		this.arrived = arrived;
	}

	public Processus(String name, float ar, float rt, float iot, float iolast, int prio) {
		this.nameProc = name;
		this.arrive_t = ar;
		this.remain_t = rt;
		this.total_t = rt;
		this.ioAt_t = iot;
		this.ioLastF_t = iolast;
		this.priority_l = prio;
		this.finished = false;

		this.arrived = false;
		this.actif = false;
		this.block = false;
		this.stateProcString = " ";

		// Initialisation des valeurs d'origine
		this.originalArrive_t = ar;
		this.originalRemain_t = rt;
		this.originalIoAt_t = iot;
		this.originalIoLastF_t = iolast;
		this.originalPriority_l = prio;
	}

	// Méthode de réinitialisation
	public void reset() {
		this.arrive_t = originalArrive_t;
		this.remain_t = originalRemain_t;
		this.total_t = originalRemain_t;
		this.ioAt_t = originalIoAt_t;
		this.ioLastF_t = originalIoLastF_t;
		this.priority_l = originalPriority_l;

		this.finished = false;
		this.arrived = false;
		this.actif = false;
		this.block = false;
		this.stateProcString = " ";
	}

	// Constructeur
	public void setStateProcString(String stateProcString) {
		this.stateProcString = stateProcString;
	}

	// Getters
	public String getNameProc() {
		return nameProc;
	}

	public float getArrive_t() {
		return arrive_t;
	}

	public float getRemain_t() {
		return remain_t;
	}

	public float getTotal_t() {
		return total_t;
	}

	public float getIoAt_t() {
		return ioAt_t;
	}

	public float getIoLastF_t() {
		return ioLastF_t;
	}

	public int getPriority_l() {
		return priority_l;
	}

	public boolean isFinished() {
		return finished;
	}

	public boolean isActif() {
		return actif;
	}

	public boolean isArrived() {
		return arrived;
	}
	public boolean isBlocked() {
		return block;
	}

	public String getStateProcString() {
		return stateProcString;
	}

	// Méthode toString
	@Override
	public String toString() {
		return "Processus {" +
				"Nom='" + nameProc + '\'' +
				", Arrivée=" + arrive_t +
				", Temps restant=" + remain_t +
				", Temps total=" + total_t +
				", I/O début=" + ioAt_t +
				", I/O durée=" + ioLastF_t +
				", Priorité=" + priority_l +
				", Terminé=" + finished +
				", Actif=" + actif +
				", Arrivé=" + arrived +
				", État='" + stateProcString + '\'' +
				'}';
	}
}