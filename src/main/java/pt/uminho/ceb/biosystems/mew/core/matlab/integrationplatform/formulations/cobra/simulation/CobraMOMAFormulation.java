package pt.uminho.ceb.biosystems.mew.core.matlab.integrationplatform.formulations.cobra.simulation;

import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import pt.uminho.ceb.biosystems.mew.core.matlab.integrationplatform.connection.converter.ConnectionFormulation;
import pt.uminho.ceb.biosystems.mew.core.matlab.integrationplatform.connection.matlab.MatlabConnection;
import pt.uminho.ceb.biosystems.mew.core.matlab.integrationplatform.exceptions.CobraMatlabFormulationException;
import pt.uminho.ceb.biosystems.mew.core.model.steadystatemodel.ISteadyStateModel;
import pt.uminho.ceb.biosystems.mew.core.simulation.components.FluxValueMap;
import pt.uminho.ceb.biosystems.mew.core.simulation.components.SimulationProperties;
import pt.uminho.ceb.biosystems.mew.core.simulation.components.SteadyStateSimulationResult;
import pt.uminho.ceb.biosystems.mew.core.simulation.formulations.exceptions.ManagerExceptionUtils;

public class CobraMOMAFormulation extends ConnectionFormulation {
	
	Boolean				isMaximized	= true;
	Map<String, Double>	obj_coef	= null;
	String				modelNameFromOptFlux;
	String				modelNameWT;
						
	public CobraMOMAFormulation(ISteadyStateModel model) throws MatlabConnectionException, MatlabInvocationException {
		super(model, new MatlabConnection());
		initFormulation(model);
	}
	
	private void initFormulation(ISteadyStateModel model) {
		modelNameFromOptFlux = model.getId();
		if (modelNameFromOptFlux == null || modelNameFromOptFlux.equals(""))
			modelNameFromOptFlux = "model";
		if (modelNameFromOptFlux.startsWith("."))
			modelNameFromOptFlux = "DOT" + modelNameFromOptFlux;
			
		modelNameFromOptFlux = model.getId();
		modelNameWT = modelNameFromOptFlux + "_WT";
	}
	
	@Override
	protected Map<String, Object> createConverterParameteres() {
		return null;
	}
	
	@Override
	protected void initPropsKeys() {
		super.initPropsKeys();
		mandatoryProps.add(SimulationProperties.IS_MAXIMIZATION);
		possibleProperties.add(SimulationProperties.OBJECTIVE_FUNCTION);
	}
	
	@Override
	public void prepareMatlabEnvironment() {
		try {
			isMaximized = ManagerExceptionUtils.testCast(properties, Boolean.class, SimulationProperties.IS_MAXIMIZATION, false);
		} catch (Exception e) {
			e.printStackTrace();
			isMaximized = true;
		}
		
		try {
			obj_coef = ManagerExceptionUtils.testCast(properties, Map.class, SimulationProperties.OBJECTIVE_FUNCTION, false);
		} catch (Exception e) {
			e.printStackTrace();
			obj_coef = new HashMap<String, Double>();
			obj_coef.put(model.getBiomassFlux(), 1.0);
		}
		
//		if(obj_coef.size() == 1 && !obj_coef.containsKey(model.getBiomassFlux()))
//			getConverter().runFunction("changeObjective", Arrays.asList(new String[]{modelNameFromOptFlux, "'"+obj_coef.entrySet().iterator().next().getKey()+"'"}), 
//					Arrays.asList(new String[]{modelNameFromOptFlux}));
//		
//		if(obj_coef.size() == 1 && !obj_coef.containsKey(model.getBiomassFlux()))
//			getConverter().runFunction("changeObjective", Arrays.asList(new String[]{modelNameWT, "'"+obj_coef.entrySet().iterator().next().getKey()+"'"}), 
//					Arrays.asList(new String[]{modelNameWT}));
		
		try {
			converter.sendModel(model, modelNameWT);
		} catch (Exception e) {
			throw new CobraMatlabFormulationException(e);
		}
	}
	
	@Override
	public void executeSimulationCommand() {
		
		try {
			getConverter().runFunction("MOMA",
					Arrays.asList(new String[] { modelNameWT, modelNameFromOptFlux }),
					Arrays.asList(new String[] { "solutionDel", "solutionWT", "totalFluxDiff", "solStatus" }));
		} catch (Exception e) {
			throw new CobraMatlabFormulationException(e);
		}
	}
	
	@Override
	public SteadyStateSimulationResult parseMatlabOutput() {
		SteadyStateSimulationResult result = new SteadyStateSimulationResult(model, "", null);
		
		try {
			
			double[] primal = getConverter().getVariableDoubleList("solutionDel.x");
			
			String[] rxns = getConverter().getVariableStringList(modelNameWT + ".rxns");
			
			FluxValueMap fluxValues = new FluxValueMap();
			for (int i = 0; i < rxns.length; i++)
				fluxValues.put(rxns[i], primal[i]);
				
			// Função objetivo
//			getConverter().runFunction("checkObjective", Arrays.asList(new String[]{modelNameWT}), Arrays.asList(new String[]{"objectiveAbbr"}));
//			String oFString = getConverter().getVariableString("objectiveAbbr");
//			result.setOFString(oFString);
//			
//			result.setOFvalue((getConverter().getVariableDoubleList("solutionDel.f"))[0]);
//			result.setSolverOutput((getConverter().getVariableString("solutionWT.solver")));
//			result.setFluxValues(fluxValues);
			
			//getConverter().runFunction("checkObjective", Arrays.asList(new String[]{modelNameWT}), Arrays.asList(new String[]{"objectiveAbbr"}));
			//String oFString = getConverter().getVariableString("objectiveAbbr");
			result.setOFString("Σ(v-wt)²");
//			getConverter().sendDouble("DOUBLE", 10000.5);
//			System.out.println("HERE!");
//			System.out.println((getConverter().getVariableDoubleList("DOUBLE"))[0]+" Valor");
//			
//			double resultDiff = ;
//			System.out.println(resultDiff);

//			result.setOFvalue((getConverter().getVariableDoubleList("solutionDel.f"))[0]);
			result.setOFvalue((getConverter().getVariableDoubleList("totalFluxDiff"))[0]);
			result.setSolverOutput((getConverter().getVariableString("solutionWT.solver")));
			result.setFluxValues(fluxValues);
			
			result.setMethod("Cobra MOMA");
			return result;
			
		} catch (Exception e) {
			throw new CobraMatlabFormulationException(e);
		}
	}
	
	@Override
	public void clearAllProperties() {
		// TODO Auto-generated method stub
		
	}
}
