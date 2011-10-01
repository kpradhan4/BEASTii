package beast.inference;

import java.io.File;
import java.io.PrintStream;

import beast.core.Description;
import beast.core.Input;
import beast.core.MCMC;
import beast.util.Randomizer;

@Description("MCMC chain that synchronises through files. Can be used with ParticleFilter instead of plain MCMC.")
public class MCMCParticle extends MCMC {
	public Input<Integer> m_stepSize = new Input<Integer>("stepsize", "number of samples before switching state (default 10000)", 10000);

	int m_nStepSize;
	String m_sParticleDir;
	int k;
	File f;
	File f2;
	
	@Override
	public void initAndValidate() throws Exception {
		m_nStepSize = m_stepSize.get();
		m_sParticleDir = System.getProperty("beast.particle.dir");
		System.err.println("MCMCParticle living in " + m_sParticleDir);
		

		super.initAndValidate();
		k = 0;
	}
	
	
	@Override
	protected void callUserFunction(int iSample) {
		if ((iSample +1) % m_nStepSize == 0) {
			f2 = new File(m_sParticleDir + "/particlelock" + k);
			f = new File(m_sParticleDir + "/threadlock" + k);
			k++;
			try {
				state.storeToFile();
				operatorSet.storeToFile();

				PrintStream out = new PrintStream(f2);
				out.print("X");
				out.close();
				while (!f.exists()) {
					Thread.sleep(ParticleLauncherByFile.TIMEOUT);
					System.out.println(iSample + ": waiting for " + f.getAbsolutePath());
				}
				System.out.println(iSample + ": " + f.getAbsolutePath() + " exists");
				if (f.delete()) {
					System.out.println(iSample + ": " + f.getAbsolutePath() + " deleted");
				}
				
				Randomizer.setSeed(Randomizer.getSeed());
	        	System.out.println("Seed = " + Randomizer.getSeed());
	        	
				state.restoreFromFile();
				operatorSet.restoreFromFile();
				fOldLogLikelihood = robustlyCalcPosterior(posterior);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
}