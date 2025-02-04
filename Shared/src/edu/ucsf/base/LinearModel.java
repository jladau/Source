package edu.ucsf.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.regression.*;
import org.apache.commons.math3.util.CombinatoricsUtils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import static edu.ucsf.base.ExtendedMath.*;
import static java.lang.Math.*;

/**
 * Fits linear model using apache math module.
 * @author jladau
 */

public class LinearModel{

	//ols1 = apache multiple regresssion object
	//mapColumn(sVariable) = returns the column in the original data matrix with the given variable
	//mapColumnX(sVariable) = returns the column in the x-matrix with the given variable
	//rgdData = data in double format
	//iRows = number of rows of data
	//rgdX = previous predictor matrix
	//rgdY = previous response matrix
	//rgsPredictors = current predictors
	//sResponse = current response
	//rgdCoefficients = current coefficients
	//mapCoefficients = current coefficients
	//lstSamples = sample IDs
	//tblData = data table
	//setVariables = set of all variables
	
	private HashSet<String> setVariables;
	protected HashMap<String,Double> mapCoefficients;
	private OLSMultipleLinearRegression ols1;
	private Map<String,Integer> mapColumn;
	private Map<String,Integer> mapColumnX;
	private double rgdData[][];
	private double[] rgdCoefficients;
	private int iRows;
	protected double rgdX[][];
	protected double rgdY[];
	private String rgsPredictors[];
	protected String sResponse;
	private ArrayList<String> lstSamples;
	public Table<String,String,Double> tblData;
	
	/**
	 * Constructor
	 * @param tblData Data. Rows represent predictor variables or the response variable; columns are observations.
	 * @param setPredictors Set of predictor variables to use. Variables included in data table but excluded from this set will be excluded.
	 * @param sResponse Response variable to use.
	 */
	public LinearModel(Table<String,String,Double> tblData, String sResponse, Set<String> setPredictors) throws Exception{
		initialize(tblData, sResponse, setPredictors, false);
	}

	/**
	 * Constructor
	 * @param tblData Data. Rows represent predictor variables or the response variable; columns are observations.
	 * @param setPredictors Set of predictor variables to use. Variables included in data table but excluded from this set will be excluded.
	 * @param sResponse Response variable to use.
	 * @param bNoIntercept Intercept
	 */
	public LinearModel(Table<String,String,Double> tblData, String sResponse, Set<String> setPredictors, boolean bNoIntercept) throws Exception{
		initialize(tblData, sResponse, setPredictors, bNoIntercept);
	}
	
	private void initialize(Table<String,String,Double> tblData, String sResponse, Set<String> setPredictors, boolean bNoIntercept) throws Exception{
		
		//iCol = current column
		//itr1 = iterator
		//s1 = current row or column id
		
		Iterator<String> itr1;
		String s1;
		int iCol;
		
		//initializing multiple regression object
		ols1 = new OLSMultipleLinearRegression();
		ols1.setNoIntercept(bNoIntercept);
		
		//loading set of variables
		setVariables = new HashSet<String>(setPredictors);
		setVariables.add(sResponse);
		
		//removing samples that have NaN values
		lstSamples = new ArrayList<String>(tblData.columnKeySet());
		itr1 = lstSamples.iterator();
		while(itr1.hasNext()){
			s1 = itr1.next();
			for(String s:setVariables){
				if(Double.isNaN(tblData.get(s, s1))){
					itr1.remove();
					break;
				}
			}
		}
		
		//initializing objects
		rgdData = new double[lstSamples.size()][setVariables.size()];
		mapColumn = new HashMap<String,Integer>();
		iCol = 0;
		for(String s:setVariables){
			
			//loading column for current data name
			mapColumn.put(s, iCol);
			for(int i=0;i<lstSamples.size();i++){
				
				//loading data
				rgdData[i][iCol]=tblData.get(s, lstSamples.get(i));			
			}
			iCol++;
		}
	
		//loading number of rows of data
		iRows = rgdData.length;
		
		//checking if response is valid
		if(!mapColumn.containsKey(sResponse)){
			throw new Exception("Response variable not found.");
		}

		//loading response variable
		rgdY = new double[iRows];
		iCol = mapColumn.get(sResponse);
		for(int i=0;i<iRows;i++){
			rgdY[i]=rgdData[i][iCol];
		}
		
		//initializing variables
		rgsPredictors = new String[0];
		this.sResponse = sResponse;
		
		//saving data table
		this.tblData=tblData;
		
	}
	
	/**
	 * Fits model with given response variable and predictors. Data are loaded first for given model.   
	 * @param setPredictors Predictor names.
	 */
	public void fitModel(Set<String> setPredictors) throws Exception{
		
		//iCol = current column
		//rgsPredictors = local version of predictors
		
		int iCol;
		String rgsPredictors[];
		
		//checking if predictors are valid
		for(String s:setPredictors){
			if(!mapColumn.containsKey(s)){
				throw new Exception("Predictor not found.");
			}
		}
		
		//converting to array
		rgsPredictors = setPredictors.toArray(new String[setPredictors.size()]);
		
		//loading predictor matrix
		if(rgsPredictors.length>0){
			if(rgsPredictors.length==this.rgsPredictors.length){
				
				//loading predictors
				for(int j=0;j<rgsPredictors.length;j++){
					if(rgsPredictors[j].equals(this.rgsPredictors[j])){
						continue;
					}
					iCol = mapColumn.get(rgsPredictors[j]);
					mapColumnX.put(rgsPredictors[j], j);
					for(int i=0;i<iRows;i++){
						rgdX[i][j]=rgdData[i][iCol];
					}
				}
			}else{
				
				//initializing
				rgdX = new double[iRows][setPredictors.size()];
				mapColumnX = new HashMap<String,Integer>();
				
				//loading predictors
				for(int j=0;j<rgsPredictors.length;j++){
					iCol = mapColumn.get(rgsPredictors[j]);
					for(int i=0;i<iRows;i++){
						rgdX[i][j]=rgdData[i][iCol];
					}
					mapColumnX.put(rgsPredictors[j], j);
				}
			}
		}else{
			rgdX = null;
		}
		
		//updating variables
		this.rgsPredictors = rgsPredictors;
		
		//loading data
		this.loadData(rgdX, rgdY);
		
		//clearing coefficients
		this.mapCoefficients=null;
	}

	/**
	 * Returns number of observations
	 * @return Number of observations used for model
	 */
	public int getNumberOfObservations(){
		return rgdY.length;
	}
	
	/**
	 * Checks if VIF is below threshold. Returns true if it is below threshold, false otherwise
	 * @param dThreshold Threshold.
	 * @param binDoNotCheckVIF Binary relation giving pairs of predictors for which VIF should not be checked. Usedful for quadratic models and other cases.
	 */
	public boolean checkVIF(double dThreshold, BinaryRelation<String> binDoNotCheckVIF) throws Exception{
		
		//sResponse = current response variable
		//rgdX = matrix of predictor variables
		//rgdY = response variable
		//iCol = current column
		//i1 = current predictors column
		//ols2 = regression object
		//iPredictorsCols = number of predictor columns
		
		double d1;
		String sResponse;
		int iPredictorCols; int iCol; int i1;
		double rgdX[][]; double rgdY[];
		OLSMultipleLinearRegression ols2;
		
		//loading R^2 threshold
		d1 = 1.-1./dThreshold;
		
		//checking if predictors are valid
		for(int i=0;i<rgsPredictors.length;i++){
			if(!mapColumn.containsKey(rgsPredictors[i])){
				throw new Exception("Invalid predictor variable.");
			}
		}
		
		//initializing regression object
		ols2 = new OLSMultipleLinearRegression();
		
		//looping through predictors
		for(int k=0;k<rgsPredictors.length;k++){
		
			//loading current response
			sResponse = rgsPredictors[k];	
			
			//loading number of predictors
			iPredictorCols=0;
			for(int j=0;j<rgsPredictors.length;j++){
							
				//checking if response variable
				if(rgsPredictors[j].equals(sResponse)){
					continue;
				}
				
				//checking if both variables allowed (e.g. different powers)
				if(binDoNotCheckVIF!=null){
					if(binDoNotCheckVIF.contains(rgsPredictors[j], sResponse) || binDoNotCheckVIF.contains(sResponse, rgsPredictors[j])){
						continue;
					}
				}
				
				//adding to predictor count
				iPredictorCols++;
			}
				
			//checking if at least one predictor
			if(iPredictorCols==0){
				continue;
			}
			
			//loading predictors
			rgdX = new double[iRows][iPredictorCols];
			i1=0;
			for(int j=0;j<rgsPredictors.length;j++){
				
				//checking if response variable
				if(rgsPredictors[j].equals(sResponse)){
					continue;
				}
				
				//checking if both variables allowed (e.g. different powers)
				if(binDoNotCheckVIF!=null){
					if(binDoNotCheckVIF.contains(rgsPredictors[j], sResponse) || binDoNotCheckVIF.contains(sResponse, rgsPredictors[j])){
						continue;
					}
				}
				
				iCol = mapColumn.get(rgsPredictors[j]);
				for(int i=0;i<iRows;i++){
					rgdX[i][i1]=rgdData[i][iCol];
				}
				i1++;
			}
			
			//loading response variable
			rgdY = new double[iRows];
			iCol = mapColumn.get(sResponse);
			for(int i=0;i<iRows;i++){
				rgdY[i]=rgdData[i][iCol];
			}
			
			//loading data
			ols2.newSampleData(rgdY, rgdX);
			
			//checking R^2
			if(ols2.calculateRSquared()>d1){
				return false;
			}
		
		}
		return true;
	}
	
	/**
	 * Finds variance inflation factor for each variable
	 * @return Variance inflation factor for each variable
	 */
	public HashMap<String,Double> findVIF() throws Exception{
		
		//sResponse = current response variable
		//rgdX = matrix of predictor variables
		//rgdY = response variable
		//iCol = current column
		//i1 = current predictors column
		//ols2 = regression object
		//iPredictorsCols = number of predictor columns
		//dR2 = current r^2 value
		//map1 = output
		
		HashMap<String,Double> map1;
		double dR2;
		String sResponse;
		int iPredictorCols; int iCol; int i1;
		double rgdX[][]; double rgdY[];
		OLSMultipleLinearRegression ols2;
		
		//checking if predictors are valid
		for(int i=0;i<rgsPredictors.length;i++){
			if(!mapColumn.containsKey(rgsPredictors[i])){
				throw new Exception("Invalid predictor variable.");
			}
		}

		//initializing
		map1 = new HashMap<String,Double>(rgsPredictors.length);
		
		//initializing regression object
		ols2 = new OLSMultipleLinearRegression();
		
		//looping through predictors
		for(int k=0;k<rgsPredictors.length;k++){
		
			//loading current response
			sResponse = rgsPredictors[k];	
			
			//loading number of predictors
			iPredictorCols=0;
			for(int j=0;j<rgsPredictors.length;j++){
							
				//checking if response variable
				if(rgsPredictors[j].equals(sResponse)){
					continue;
				}
				
				//checking if both variables allowed (e.g. different powers)
				if(rgsPredictors[j].contains(sResponse) || sResponse.contains(rgsPredictors[j])){
					continue;
				}
				
				//adding to predictor count
				iPredictorCols++;
			}
				
			//checking if at least one predictor
			if(iPredictorCols==0){
				continue;
			}
			
			//loading predictors
			rgdX = new double[iRows][iPredictorCols];
			i1=0;
			for(int j=0;j<rgsPredictors.length;j++){
				
				//checking if response variable
				if(rgsPredictors[j].equals(sResponse)){
					continue;
				}
				
				//checking if both variables allowed (e.g. different powers)
				if(rgsPredictors[j].contains(sResponse) || sResponse.contains(rgsPredictors[j])){
					continue;
				}
				
				iCol = mapColumn.get(rgsPredictors[j]);
				for(int i=0;i<iRows;i++){
					rgdX[i][i1]=rgdData[i][iCol];
				}
				i1++;
			}
			
			//loading response variable
			rgdY = new double[iRows];
			iCol = mapColumn.get(sResponse);
			for(int i=0;i<iRows;i++){
				rgdY[i]=rgdData[i][iCol];
			}
			
			//loading data
			ols2.newSampleData(rgdY, rgdX);
			
			//saving value
			try{
				dR2 = ols2.calculateRSquared();
				if(dR2<1){
					map1.put(sResponse,1./(1.-dR2));
				}else{
					map1.put(sResponse,Double.NaN);
				}
			}catch(Exception e){
				map1.put(sResponse,Double.NaN);
			}
		}
		
		return map1;
		
	}
	
	
	/**
	 * Finds variance inflation factor.
	*/
	//TODO write unit test
	public double findMaximumVIF() throws Exception{
		
		//sResponse = current response variable
		//rgdX = matrix of predictor variables
		//rgdY = response variable
		//iCol = current column
		//i1 = current predictors column
		//ols2 = regression object
		//iPredictorsCols = number of predictor columns
		//dMax = maximum R^2 value
		//dR2 = current r^2 value
		
		double dR2;
		double dMax;
		String sResponse;
		int iPredictorCols; int iCol; int i1;
		double rgdX[][]; double rgdY[];
		OLSMultipleLinearRegression ols2;
		
		//checking if predictors are valid
		for(int i=0;i<rgsPredictors.length;i++){
			if(!mapColumn.containsKey(rgsPredictors[i])){
				throw new Exception("Invalid predictor variable.");
			}
		}
		
		//initializing regression object
		ols2 = new OLSMultipleLinearRegression();
		
		//initializing maximum value
		dMax = Double.MIN_VALUE;
		
		//looping through predictors
		for(int k=0;k<rgsPredictors.length;k++){
		
			//loading current response
			sResponse = rgsPredictors[k];	
			
			//loading number of predictors
			iPredictorCols=0;
			for(int j=0;j<rgsPredictors.length;j++){
							
				//checking if response variable
				if(rgsPredictors[j].equals(sResponse)){
					continue;
				}
				
				//checking if both variables allowed (e.g. different powers)
				if(rgsPredictors[j].contains(sResponse) || sResponse.contains(rgsPredictors[j])){
					continue;
				}
				
				//adding to predictor count
				iPredictorCols++;
			}
				
			//checking if at least one predictor
			if(iPredictorCols==0){
				continue;
			}
			
			//loading predictors
			rgdX = new double[iRows][iPredictorCols];
			i1=0;
			for(int j=0;j<rgsPredictors.length;j++){
				
				//checking if response variable
				if(rgsPredictors[j].equals(sResponse)){
					continue;
				}
				
				//checking if both variables allowed (e.g. different powers)
				if(rgsPredictors[j].contains(sResponse) || sResponse.contains(rgsPredictors[j])){
					continue;
				}
				
				iCol = mapColumn.get(rgsPredictors[j]);
				for(int i=0;i<iRows;i++){
					rgdX[i][i1]=rgdData[i][iCol];
				}
				i1++;
			}
			
			//loading response variable
			rgdY = new double[iRows];
			iCol = mapColumn.get(sResponse);
			for(int i=0;i<iRows;i++){
				rgdY[i]=rgdData[i][iCol];
			}
			
			//loading data
			ols2.newSampleData(rgdY, rgdX);
			
			//checking R^2
			dR2 = ols2.calculateRSquared();
			if(dR2>dMax){
				dMax=dR2;
			}
		}
		return 1./(1.-dMax);
	}

	//TODO write unit test
	/**
	 * Finds standardized coeffcients.
	 * @return Map where keys are predictor variables, values are standardized coefficients.
	 */
	public HashMap<String,Double> findStandardizedCoefficientEstimates(){
		
		//mapOut = output
		//dSy = standard deviation of response variables
		//dSx = standard deviation of current predictor
		//lst1 = list of current values for calculating standard deviation
		
		HashMap<String,Double> mapOut;
		double dSx;
		double dSy;
		ArrayList<Double> lst1;
		
		//loading standard deviation of response variables
		dSy = ExtendedMath.standardDeviationP(rgdY);
		
		//initializing output
		mapOut = new HashMap<String,Double>();
		
		//finding coefficients
		if(mapCoefficients==null){
			mapCoefficients=findCoefficientEstimates();
		}
		
		//finding values for each predictor
		for(String s:rgsPredictors){
			
			lst1 = new ArrayList<Double>(rgdX.length);
			for(int i=0;i<rgdX.length;i++){
				lst1.add(rgdX[i][this.mapColumnX.get(s)]);
			}
			dSx=ExtendedMath.standardDeviationP(lst1);
			mapOut.put(s, mapCoefficients.get(s)*dSx/dSy);
		}
		return mapOut;
	}
	
	/**
	 * Finds lmg estimates of predictor importance as per Kruskal, 1987, The American Statistician, 41: 6-10 and others. LMG scores are normalized to sum to 1. 
	 * @return Map where keys are predictor variables, values are lmg estimates of importance.
	 */
	public HashMap<String,Double> findLMG() throws Exception{
		
		//setPower = set of all subsets of predictors
		//lnm1 = linear model for calculating subset R^2
		//mapR2 = r^2 value for given model
		//mapOut = output
		//iN = total count
		//d1 = current output value
		//dTotalCalculated = total calculated
		//dTotal = total
		
		double dTotalCalculated;
		double dTotal=Double.NaN;
		double d1;
		HashMap<String,Double> mapOut;
		Set<Set<String>> setPower;
		LinearModel lnm1;
		HashMap<Set<String>,Double> mapR2;
		int iN;
		
		//loading power set
		setPower = Sets.powerSet(new HashSet<String>(Arrays.asList(rgsPredictors)));
				
		//loading linear model
		lnm1 = new LinearModel(tblData, sResponse, mapColumn.keySet());
		
		//looping through subsets of model
		mapR2 = new HashMap<Set<String>,Double>();
		for(Set<String> set1:setPower){
			if(set1.size()>0){
				lnm1.fitModel(set1);
				mapR2.put(set1, lnm1.findRSquared());
				if(set1.size()==rgsPredictors.length){
					dTotal=mapR2.get(set1);
				}
			}else{
				mapR2.put(set1, 0.);
			}
		}
		
		//loading contributions
		mapOut = new HashMap<String,Double>();
		dTotalCalculated=0;
		iN = rgsPredictors.length;
		for(String s:rgsPredictors){
			d1=0.;
			for(Set<String> set1:setPower){
				if(set1.size()>0){
					if(set1.contains(s)){
						d1+=mapR2.get(set1)/((double) iN * (double) CombinatoricsUtils.binomialCoefficientDouble(iN-1, set1.size()-1));
					}else{
						d1-=mapR2.get(set1)/((double) iN * (double) CombinatoricsUtils.binomialCoefficientDouble(iN-1, set1.size()));
					}
				}
			}
			mapOut.put(s, d1/dTotal);
			dTotalCalculated+=d1/dTotal;
		}
		if(rgsPredictors.length>0){
			if(Math.abs(1.-dTotalCalculated)>0.00001){
				throw new Exception("Hierarchical partitioning total does not match full model R^2 (" + dTotalCalculated + "). Exiting.");
			}
		}
		return mapOut;
	}
	
	/**
	 * Finds R^2 of current model.
	 * @return R^2 value
	 */
	public double findRSquared(){
		
		//lst1 = list of response variables
		
		if(rgdX!=null){	
			return ols1.calculateRSquared();
		}else{
			return 1. - sumOfPowersMeanCentered(rgdY, 2)/findTSS();
		}
	}
	
	/**
	 * Finds Adjusted R^2 of current model.
	 * @return Adjusted R^2 value
	 */
	public double findAdjustedRSquared(){
	
		if(rgdX!=null){
			return ols1.calculateAdjustedRSquared();
		}else{
			return findRSquared();
		}
	}
	
	/**
	 * Finds the total sum of squares
	 * @param sResponse Response variable
	 * @return Total sum of squares
	 */
	public double findTSS(){
		
		//iCol = current column
		//dMean = mean value
		//dOut = output
		
		double dMean; double dOut;
		int iCol;
		
		//checking if response is valid
		if(!mapColumn.containsKey(sResponse)){
			System.out.println("WARNING: no model found or invalid response variable. Exiting.");
			return Double.NaN;
		}
		
		//loading response variable
		iCol = mapColumn.get(sResponse);
		dMean = 0;
		for(int i=0;i<iRows;i++){
			dMean+=rgdData[i][iCol];
		}
		dMean=dMean/((double) rgdData.length);
		dOut = 0;
		for(int i=0;i<iRows;i++){
			dOut+=pow(rgdData[i][iCol]-dMean,2);
		}
		return dOut;
	}
	
	public double findPRESS(){
		
		//rgdResiduals = residuals
		//rgmHat = hat matrix
		//d1 = output
		//d2 = current hat matrix value
		
		
		double rgdResiduals[] = null;
		RealMatrix rgmHat = null;
		double d1; double d2;
		
		if(rgdX!=null){
		
			//loading residuals and hat matrix
			try{
				rgdResiduals = ols1.estimateResiduals();
			}catch(Exception e){
				for(int i=1;i<rgdX.length;i++){
					for(int j=0;j<rgdX[0].length;j++){
						if(rgdX[i][j]!=rgdX[0][j] || rgdY[i]!=rgdY[0]){
							e.printStackTrace();
						}
					}
				}
				return 0.;
			}
			try{
				rgmHat = ols1.calculateHat();
			}catch(Exception e){
				e.printStackTrace();
			}
			
			//loading output
			d1 = 0;
			for(int i=0;i<rgdResiduals.length;i++){
				d2 = rgmHat.getEntry(i, i);
				if(abs(d2-1.)<0.0000000001){
					if(abs(rgdResiduals[i])>0.000000001){
						System.out.println("ERROR: hat matrix entry equal to 1.");
					}
				}else{
					if(abs(1.-d2)>0.0000000001){
						d1+=pow(rgdResiduals[i]/(1.-d2),2);
					}
				}
			}
		}else{
			d1 = sumOfPowersMeanCentered(rgdY, 2.)*pow((double) rgdY.length/((double) rgdY.length-1.),2.);
		}
			
		//returning result
		return d1;
	}

	/**
	 * Finds prediction
	 * @param mapPredictors Keys are predictor names, values are predictor values
	 * @return Prediction 
	 */
	public double findPrediction(HashMap<String,Double> mapPredictors){
		
		//d1 = output
		
		double d1;
		
		if(mapCoefficients==null){
			mapCoefficients=findCoefficientEstimates();
		}
		d1 = mapCoefficients.get("(Intercept)");
		for(String t:mapCoefficients.keySet()){
			if(!t.equals("(Intercept)")){
				
				//***************************
				//System.out.println(t + ":" + mapPredictors.get(t));
				//***************************
				
				d1+=mapCoefficients.get(t)*mapPredictors.get(t);
				if(Double.isNaN(d1)){
					return Double.NaN;
				}
			}
		}
		return d1;
	}
	
	/**
	 * Resamples residuals to create new data set of response variables
	 * @param iResamples Number of resamples
	 * @return Table between observation ID and resample iteration and new response value
	 */
	public HashBasedTable<String,Integer,Double> resampleNonparametric(int iResamples){
		
		//rgdResiduals = matrix of residuals
		//tblOut(sSample) = returns the yhat value associated with the specified sample
		//mapYHat(sSample) = Y Hat matrix
		//rnd1 = random number generator
		
		Random rnd1;
		double[] rgdResiduals = null;
		HashBasedTable<String,Integer,Double> tblOut;
		HashMap<String,Double> mapYHat;
		
		rgdResiduals = findResiduals();
		tblOut = HashBasedTable.create(rgdResiduals.length, iResamples);
		mapYHat = findPredictedValues();
		rnd1 = new Random();
		for(int i=0;i<lstSamples.size();i++){
			for(int j=0;j<iResamples;j++){
				tblOut.put(lstSamples.get(i), j, mapYHat.get(lstSamples.get(i))+rgdResiduals[rnd1.nextInt(rgdResiduals.length)]);
			}
		}
		return tblOut;
	}
	
	
	/**
	 * Create new data set of response variables assuming linear model with fitted intercept, slope, and variance
	 * @param iResamples Number of resamples
	 * @return Table between observation ID and resample iteration and new response value
	 */
	public HashBasedTable<String,Integer,Double> resampleParametric(int iResamples){
		
		//rgdResiduals = matrix of residuals
		//tblOut(sSample) = returns the yhat value associated with the specified sample
		//mapYHat(sSample) = Y Hat matrix
		//rnd1 = random number generator
		//dStDev = estimated variance
		
		Random rnd1;
		double[] rgdResiduals = null;
		HashBasedTable<String,Integer,Double> tblOut;
		HashMap<String,Double> mapYHat;
		double dStDev;
		
		rgdResiduals = findResiduals();
		tblOut = HashBasedTable.create(rgdResiduals.length, iResamples);
		mapYHat = findPredictedValues();
		try{
			dStDev = Math.sqrt(ols1.estimateErrorVariance());
		}catch(Exception e){
			dStDev = Math.sqrt(Math.pow(ExtendedMath.standardDeviationP(rgdY),2)*((double) rgdY.length)/((double) rgdY.length-1.));
		}
		rnd1 = new Random();
		for(int i=0;i<lstSamples.size();i++){
			for(int j=0;j<iResamples;j++){
				tblOut.put(lstSamples.get(i), j, mapYHat.get(lstSamples.get(i))+rnd1.nextGaussian()*dStDev);
			}
		}
		return tblOut;
	}
	
	//TODO write unit test
	/**
	 * Finds observed values (Y)
	 * @return Observed values
	 */
	public HashMap<String,Double> getObservedValues(){
		
		//mapOut = output
		
		HashMap<String,Double> mapOut;
		
		mapOut = new HashMap<String,Double>(rgdY.length);
		for(int i=0;i<rgdY.length;i++){
			mapOut.put(lstSamples.get(i), rgdY[i]);
		}
		return mapOut;
	}
	
	//TODO write unit test
	/**
	 * Gets currently loaded predictors
	 * @return Table mapping samples and predictor names to predictor values
	 */
	public HashBasedTable<String,String,Double> getPredictors(){
		
		//tbl1Out = output
		//sSample = current sample
		
		HashBasedTable<String,String,Double> tbl1;
		String sSample;
		
		tbl1 = HashBasedTable.create();
		for(int i=0;i<rgdX.length;i++){
			sSample = lstSamples.get(i); 
			for(String s:mapColumnX.keySet()){
				tbl1.put(sSample, s, rgdX[i][mapColumnX.get(s)]);
			}
		}
		return tbl1;
	}
	
	
	/**
	 * Finds predicted values (YHat)
	 * @return Predicted values
	 */
	public HashMap<String,Double> findPredictedValues(){
		
		//rgdResiduals = matrix of residuals
		//mapOut = output
		
		double[] rgdResiduals;
		HashMap<String,Double> mapOut;
		
		rgdResiduals = findResiduals();
		mapOut = new HashMap<String,Double>(rgdResiduals.length);
		for(int i=0;i<rgdResiduals.length;i++){
			mapOut.put(lstSamples.get(i), rgdY[i]-rgdResiduals[i]);
		}
		return mapOut;
	}
	
	private double[] findResiduals(){
		
		//rgdOut = output
		//dMean = mean value
		
		double rgdOut[];
		double dMean;
		
		if(rgdX==null){
			dMean = ExtendedMath.mean(rgdY);
			rgdOut = new double[rgdY.length];
			for(int i=0;i<rgdOut.length;i++){
				rgdOut[i] = rgdY[i]-dMean;
			}
			return rgdOut;
		}else{
			return ols1.estimateResiduals();
		}
		
	}
	
	
	public HashMap<String,Double> findCoefficientEstimates(){
		
		//mapOut = output
		
		HashMap<String,Double> mapOut;
		
		if(rgdX!=null){
			this.rgdCoefficients = ols1.estimateRegressionParameters();
			mapOut = new HashMap<String,Double>(rgdCoefficients.length+1);
			if(rgsPredictors.length==rgdCoefficients.length-1) {
				mapOut.put("(Intercept)",rgdCoefficients[0]);
				for(int j=0;j<rgsPredictors.length;j++){
					mapOut.put(rgsPredictors[j],rgdCoefficients[j+1]);
				}
			}else{
				for(int j=0;j<rgsPredictors.length;j++){
					mapOut.put(rgsPredictors[j],rgdCoefficients[j]);
				}
			}
			
		}else{
			mapOut = new HashMap<String,Double>();
			mapOut.put("(Intercept)",mean(rgdY));
		}
		return mapOut;
 	}
	
	/**
	 * Loads predictor variables
	 * @param rgdX Predictor variables.
	 * @param rgdY Response variables.
	 */
	protected void loadData(double rgdX[][], double rgdY[]){
		if(rgdX!=null){		
			ols1.newSampleData(rgdY, rgdX);
		}
	}
}
