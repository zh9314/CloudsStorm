import topologyAnalysis.dataStructure.EC2.EC2VM;

public class runTest implements Runnable{
		
		private EC2VM vm;
		
		public runTest(EC2VM curVM){
			this.vm = curVM;
		}
		
		public void modify(){
			this.vm.AMI = "success";
		}

		@Override
		public void run() {
			modify();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}