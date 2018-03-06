package commonTool;

public class Values {
	public class STStatus{
		public static final String running = "running";
		public static final String failed = "failed";
		public static final String unknown = "unknown";
		public static final String fresh = "fresh";
		public static final String stopped = "stopped";
		public static final String deleted = "deleted";
	}
	public class Options{
		///following are used for 'put' and 'get'
		public static final String srcPath = "Src";
		public static final String dstPath = "Dst";
		
		///following are used for 'hscale' in vm level and sub-topology level
		public static final String requstID = "ReqID";
		public static final String cloudProivder = "CP";
		public static final String domain = "DC";
		public static final String scaledTopology = "ScaledSTName";
		public static final String subTopologyClass = "STClass";
		public static final String sEngineClass = "SEClass";
		public static final String vEngineClass = "VEClass";
		public static final String scalingUpDown = "UpDown";
		
		///following are used for 'vscale'
		public static final String targetCPU = "CPU";
		public static final String targetMEM = "MEM";
	}
}
