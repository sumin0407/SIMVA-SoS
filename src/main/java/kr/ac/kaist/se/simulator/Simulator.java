package kr.ac.kaist.se.simulator;

import simulator.Action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * System of Systems Simulator
 * Created by Junho on 2016-08-01.
 * Output model : System.out.println(Integer.toString(tick) + " "
 * + a.getPerformer() + " " + a + " " + cs.getAccumulatedSoSBenefit());
 */
public final class Simulator {

    private ArrayList<BaseConstituent> csList = null;
    private BaseConstituent manager = null;
    private Environment env = null;
    private int tick;
    private int minimumActionCost;

    public Simulator(BaseConstituent[] CSs, BaseConstituent manager, Environment env){
        this.csList = new ArrayList<BaseConstituent>();
        this.csList.addAll(Arrays.asList(CSs));
        this.manager = manager;
        this.env = env;
        this.tick = 0;

        this.minimumActionCost = Integer.MAX_VALUE - 100000;
        for(BaseConstituent CS: this.csList){
        for(Action a : CS.getCapability()){
            if(CS.getCost(a) < minimumActionCost)
                minimumActionCost = CS.getCost(a);
        }
    }
}

    /**
     * simulate the model n steps Every steps, reset the environment and states
     * @param steps the number of simulating steps
     */
    public void execute(int steps){
        //for(int i=0 ; i< steps; i++){
            this.procedure();
        //}
    }

    /**
     * Simulation Procedure function from "Simulation and SMC of Logic-Based MAS Models"
     */
    private void procedure(){
        boolean endCondition = false;
        ArrayList<Action> immediateActions = new ArrayList<Action>();
        ArrayList<Action> actions = new ArrayList<Action>();

        while(!endCondition){
            actions.clear();

            immediateActions.clear();

            for(BaseConstituent cs : this.csList){ // Get immediate candidate action
                Action a = cs.step();
                if(a == null)
                    continue;
                if(a.getDuration() == 0){ // This action is immediate
                    // Immediate action: making a decision
                    // insert to immediateAction set
                    immediateActions.add(a);
                }else {
                    // insert to normal action set
                    // Normal actions
                    actions.add(a);
                }
            }

            if(this.manager != null){
                Action a = this.manager.step();
                if(a != null){
                    if(a.getDuration() == 0)
                        immediateActions.add(a);
                    else
                        actions.add(a);
                }
            }

            Collections.shuffle(immediateActions);
            this.progress(immediateActions, Action.TYPE.IMMEDIATE); // Choose

            this.generateExogenousActions(); // Environment action
            Collections.shuffle(actions);
            this.progress(actions, Action.TYPE.NORMAL);

            endCondition = this.evaluateProperties();
        }
        System.out.println("Final Tick " + this.tick);
        int SoSBenefit = 0;
        for(BaseConstituent CS : this.csList){
            SoSBenefit += CS.getAccumulatedSoSBenefit();
        }
        System.out.println("SoS benefit " + SoSBenefit);
        for(BaseConstituent CS: this.csList){
            System.out.println(CS + " gets " + CS.getAccumulatedBenefit() + " benefits");
        }
    }

    private void increaseTick(int minimumElapsedTime){
        if(minimumElapsedTime > 0)
            this.tick += minimumElapsedTime;
    }

    private boolean evaluateProperties(){
        /*
         * 1. Check whether is world ended?
         * 1-1. If all CS has the cost which is less than the minimum cost of actions to execute
         *
         */
        boolean verdict = true;
        for(BaseConstituent CS: this.csList){
            if(CS.getStatus() != BaseConstituent.Status.END)
                verdict = false;
        }
        if(this.tick > 10000){
            verdict = true;
            System.out.println("10000 step is done");
        }
        return verdict;
    }

    private void generateExogenousActions(){
        /*
         * Generate randomly out action
         * Notify to CS that environment generate the actions
         */
        if(env.generateAction() > 0)
            env.notifyCS();

    }

    private void progress(ArrayList<Action> actionList, Action.TYPE type){
        if(type == Action.TYPE.IMMEDIATE){
            for(Action a : actionList){
                if(a.getActionType() == Action.TYPE.IMMEDIATE) {
                    a.getPerformer().immediateAction(); // Select action
                }
            }
        }else if(type == Action.TYPE.NORMAL){
            /*
             * 1. Calculate the minimum time to elapse among action list
             * 2. Elapse the time and execute
             * 3. If the remaining time is 0, then update Cost & Benefit
             */
            int minimumElapsedTime = -1;
            if(!actionList.isEmpty()){ // If the action list is not empty
                boolean discreteCondition = true;
                for(BaseConstituent CS: this.csList){ // To get all CS are in operation.
                    if(CS.getStatus() != BaseConstituent.Status.OPERATING){
                        discreteCondition = false;
                        break;
                    }
                }
                if(discreteCondition){ // To jump the tick
                    for(Action a : actionList){
                        if(minimumElapsedTime < a.getRemainingTime())
                            minimumElapsedTime = a.getRemainingTime();
                    }// Calculate the minimum jump tick
                }else{ // If any one CS are not in operation, minimum tick will be one
                    minimumElapsedTime = 1;
                }
                for(Action a: actionList){
//                    int SoSLevelBenefit = a.getSoSBenefit();
                    a.getPerformer().normalAction(minimumElapsedTime);
                    if(a.getPerformer() == null){ // Job is done.
                        System.out.println(this.tick + minimumElapsedTime);
//                        if(this.manager != null)
//                            this.manager.addSoSLevelBenefit(SoSLevelBenefit);
                    }
                }
            }else
                minimumElapsedTime = 1;
            increaseTick(minimumElapsedTime);
        }
        env.updateActionStatus(actionList);
        actionList.clear();
    }
}