package beast.evolution.likelihood;

import java.util.List;

public class ThreadedLikelihoodCoreNative extends ThreadedLikelihoodCore {
    long m_pBEER = 0;
	int m_nStates;
	int m_nNodes;
	double SCALE = 1;

	int m_nMatrixSize;
	int m_nMatrixCount;
	protected double[][] m_fMatrices;
	protected int[] m_nMatrixID;
	int m_nMatricesToUpdate = 0;
    protected int[] m_iCurrentMatrices;
    protected int[] m_iStoredMatrices;

    public ThreadedLikelihoodCoreNative(int nStateCount) {
    	m_nStates = nStateCount;
    } // c'tor
    
	@Override
	public void initialize(int nNodeCount, int nPatternCount, int nMatrixCount, int[] weights,
			List<Integer> iConstantPatterns, int nThreads, boolean bIntegrateCategories) {
		try {
	        System.loadLibrary("BEER");
			m_pBEER = createCppBEERObject(m_nStates);
		} catch (UnsatisfiedLinkError e) {
			System.out.println(e.getMessage());
			return;
		}
		m_nNodes = nNodeCount;
		
		
		int [] iConstantPatterns2;
		int nConstantPatterns;
		if (iConstantPatterns != null) {
			nConstantPatterns = iConstantPatterns.size();
			iConstantPatterns2 = new int[nConstantPatterns];
			for (int i = 0; i < iConstantPatterns2.length; i++) {
				iConstantPatterns2[i] = iConstantPatterns.get(i);
			}
		} else {
			iConstantPatterns2 = new int[0];
			nConstantPatterns = 0;
		}
		
		initializeC(m_pBEER, nNodeCount, nPatternCount, nMatrixCount, weights, 
				iConstantPatterns2, nConstantPatterns, nThreads, bIntegrateCategories);

       m_nMatrixSize = m_nStates * m_nStates;
       m_nMatrixCount = nMatrixCount;
       m_fMatrices = new double[nNodeCount][nMatrixCount * m_nMatrixSize];
       m_nMatrixID = new int[nNodeCount];
       m_iCurrentMatrices = new int[nNodeCount];
       m_iStoredMatrices = new int[nNodeCount];
       m_nMatricesToUpdate = 0;
	}
	
	native boolean initializeC(long m_pBEER2, int nNodeCount, int nPatternCount, int nMatrixCount, int[] weights,
			int[] iConstantPatterns2, int nConstantPatterns, int nThreads, boolean bIntegrateCategories);
    
	native long createCppBEERObject(int nStateCount);
    
	@Override
	public void finalize() throws Throwable {
		finalizeC(m_pBEER);
	}
	native void finalizeC(long pBeer);
	
	@Override
	public void createNodePartials(int iNode) {
		createNodePartialsC(m_pBEER, iNode);
	}
	native void createNodePartialsC(long pBeer, int iNode);

//	@Override
//	public void setNodePartialsForUpdate(int iNode) {
//		setNodePartialsForUpdateC(m_pBEER, iNode);
//	}
//	native void setNodePartialsForUpdateC(long pBEER, int iNode);

	@Override
	public void setNodePartials(int iNode, double[] fPartials) {
		setNodePartialsC(m_pBEER, iNode, fPartials, fPartials.length);
	}
	native void setNodePartialsC(long pBeer, int iNode, double[] fPartials, int len);

	@Override
	public void setNodeStates(int iNode, int[] iStates) {
		setNodeStatesC(m_pBEER, iNode, iStates);
	}
	native void setNodeStatesC(long pBeer, int iNode, int[] iStates);

	@Override
	public void setNodeMatrixForUpdate(int iNode) {
//        m_iCurrentMatrices[iNode] = 1 - m_iCurrentMatrices[iNode];
//
//        m_nMatrixID[m_nMatricesToUpdate] = iNode * 2 + m_iCurrentMatrices[iNode];
//		m_nMatricesToUpdate++;
		setNodeMatrixForUpdateC(m_pBEER, iNode);
	}
	native void setNodeMatrixForUpdateC(long pBEER, int iNode);

	@Override
	public void setNodeMatrix(int iNode, int iMatrixIndex, double[] fMatrix) {
//        System.arraycopy(fMatrix, 0, m_fMatrices[m_nMatricesToUpdate - 1],
//        		iMatrixIndex * m_nMatrixSize, m_nMatrixSize);
		
		setNodeMatrixC(m_pBEER, iNode, iMatrixIndex, fMatrix);
	}
	native void setNodeMatrixC(long pBeer, int iNode, int iMatrixIndex, double[] fMatrix);

	@Override
	public void setUseScaling(double fScale) {
		m_bUseScaling = (fScale != 1.0);
		SCALE = fScale;
		setUseScalingC(m_pBEER, fScale);
	}
	native void setUseScalingC(long pBeer, double fScale);

	@Override
	public double getLogScalingFactor(int iPattern) {
    	if (m_bUseScaling) {
    		return -(m_nNodes/2) * Math.log(SCALE);
    	} else {
    		return 0;
    	}
	}

	@Override
	public void calculatePartials(int iNode1, int iNode2, int iNode3, int iFrom, int iTo) {
		calculatePartialsC(m_pBEER, iNode1, iNode2, iNode3, iFrom, iTo);
	}
	native void calculatePartialsC(long pBEER, int iNode1, int iNode2, int iNode3, int iFrom, int iTo);

	//@Override
//	public void integratePartials(int iNode, double[] fProportions, double[] fOutPartials, int iFrom, int iTo) {
//		integratePartialsC(m_pBEER, iNode, fProportions, fOutPartials, iFrom, iTo);
//	}
//	native void integratePartialsC(long pBEER, int iNode, double[] fProportions, double[] fOutPartials, int iFrom, int iTo);

	//@Override
//	public void calculateLogLikelihoods(double[] fPartials, double[] fFrequencies,
//			int iFrom, int iTo) {
//		calculateLogLikelihoodsC(m_pBEER, fPartials, fFrequencies, iFrom, iTo);
//	}
//	native void calculateLogLikelihoodsC(long pBEER, double[] fPartials, double[] fFrequencies, 
//			int iFrom, int iTo);

	@Override
	public void store() {
        System.arraycopy(m_iStoredMatrices, 0, m_iCurrentMatrices, 0, m_nNodes);
		storeC(m_pBEER);
	}
	native void storeC(long pBEER);

	@Override
	public void unstore() {
        System.arraycopy(m_iStoredMatrices, 0, m_iCurrentMatrices, 0, m_nNodes);
		unstoreC(m_pBEER);
	}
	native void unstoreC(long pBEER);

	@Override
	public void restore() {
        int[] iTmp1 = m_iCurrentMatrices;
        m_iCurrentMatrices = m_iStoredMatrices;
        m_iStoredMatrices = iTmp1;
		restoreC(m_pBEER);
	}
	native void restoreC(long pBEER);

	@Override
	public double calcPartialLogP(int m_iFrom, int m_iTo) {
		return calcPartialLogPC(m_pBEER, m_iFrom, m_iTo);
	}

	native double calcPartialLogPC(long pBEER, int m_iFrom, int m_iTo);

	@Override
	public double[] getPatternLogLikelihoods() {
		//throw new RuntimeException("not implemented yet");
		return getPatternLogLikelihoodsC(m_pBEER);
	}

	native double [] getPatternLogLikelihoodsC(long pBEER);


	@Override
	double calcLogP(int iThread, 
			int [] cacheNode1, int [] cacheNode2, int [] cacheNode3, int cacheNodeCount, 
			int iFrom, int iTo, int rootNodeNr, double[] proportions, double fProportionInvariant,
			double[] frequencies) {
		for (int i = 0; i <  m_nMatricesToUpdate; i++) {
			setUpMatrixC(m_pBEER, m_fMatrices[i], m_nMatrixID[i]);
		}
		m_nMatricesToUpdate = 0;
//		if (m_nMatricesToUpdate > 0) {
//			setUpMatricesC(m_pBEER, m_fMatrices, m_nMatrixID, m_nMatricesToUpdate);
//			m_nMatricesToUpdate = 0;
//		}
		
		double logP = calcLogPC(m_pBEER, iThread, cacheNode1, cacheNode2, cacheNode3, cacheNodeCount, iFrom, iTo, rootNodeNr, proportions, fProportionInvariant, frequencies);
		return logP;
	}
	native void setUpMatricesC(long pBEER, double [] fMatrices, int [] nMatrixID, int nMatrixToUpdate);
	native void setUpMatrixC(long pBEER, double [] fMatrices, int nMatrixID);

	native double calcLogPC(long pBEER, int iThread, 
			int [] cacheNode1, int [] cacheNode2, int [] cacheNode3, int cacheNodeCount, 
			int iFrom, int iTo, int rootNodeNr, double[] proportions, double fProportionInvariant,
			double[] frequencies);

	@Override
	public void calculateAllPartials(int[] iNode1, int[] iNode2, int[] iNode3, int nCacheCount, int iFrom, int iTo) {
		calculateAllPartialsC(m_pBEER, iNode1, iNode2, iNode3, nCacheCount, iFrom, iTo);
	}
	
	native void calculateAllPartialsC(long pBEER, int[] iNode1, int[] iNode2, int[] iNode3, int nCacheCount, int iFrom, int iTo);

	@Override
	public void getNodePartials(int iNode, double[] fPartials) {
		// TODO Auto-generated method stub
	}

}
