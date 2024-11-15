package mde;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.m2m.atl.common.ATL.Helper;
import org.eclipse.m2m.atl.common.ATL.Module;
import org.eclipse.m2m.atl.common.OCL.BooleanType;
import org.eclipse.m2m.atl.common.OCL.IntegerExp;
import org.eclipse.m2m.atl.common.OCL.NavigationOrAttributeCallExp;
import org.eclipse.m2m.atl.common.OCL.OclExpression;
import org.eclipse.m2m.atl.common.OCL.Operation;
import org.eclipse.m2m.atl.common.OCL.OperatorCallExp;
import org.eclipse.m2m.atl.common.OCL.OperationCallExp;
import org.eclipse.m2m.atl.common.OCL.VariableExp;
import org.eclipse.m2m.atl.common.Problem.Problem;
import org.eclipse.m2m.atl.core.emf.EMFModel;
import org.eclipse.m2m.atl.core.emf.EMFModelFactory;
import org.eclipse.m2m.atl.emftvm.Model;
import org.eclipse.m2m.atl.emftvm.compiler.AtlResourceFactoryImpl;
import org.eclipse.m2m.atl.emftvm.util.OCLOperations;
import org.eclipse.m2m.atl.engine.parser.AtlParser;
import org.eclipse.xtext.xbase.lib.Exceptions;

import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.stream.IntStream;
import java.nio.file.Path;

import zoo.*; //This is the model we're trying to solve the problems for, it's designed in xcore

public class App {
    static int magic = 100;
    static int cA,cC,cS; //count of Animals, Cages, Species

    //XMI Loader
    static EObject loadInstance(String path){
        ResourceSetImpl rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        rs.getPackageRegistry().put(ZooPackage.eNS_URI,ZooPackage.eINSTANCE);
        Resource res = rs.getResource(URI.createFileURI(path), true);
        return res.getContents().get(0);
    }

    static void saveInstance(String path, Park p2){
        ResourceSetImpl rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        rs.getPackageRegistry().put(ZooPackage.eNS_URI,ZooPackage.eINSTANCE);
        Resource res = rs.createResource(URI.createFileURI(path));
        res.getContents().add(p2);
        try{
            res.save(null);
        } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
        }
    }


    // //ATL Loader
    static Module loadModule(String path) {
        var rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("atl", new AtlResourceFactoryImpl());

        var parser = AtlParser.getDefault();
        var modelFactory = new EMFModelFactory();
        EMFModel problems = (EMFModel)modelFactory.newModel(parser.getProblemMetamodel());
        rs.getLoadOptions().put("problems", problems);

        try {

            var transfo = rs.getResource(URI.createURI(path), true);
            var fileName = Path.of(path).getFileName();

            for (var e : transfo.getErrors()) {
                System.err.println("error in " + fileName + ": " + e.getLine() + "-" + e.getColumn() + " - " + e.getMessage());
            }

            for (var e : transfo.getWarnings()) {
                System.err.println("warning in " + fileName + ": " + e.getLine() + "-" + e.getColumn() + " - " + e.getMessage());
            }

            return (Module)transfo.getContents().get(0);

        } catch (Exception e) {
            System.err.println("Error reading input file '" + path + "' : " + e.getMessage());
            for (var p : problems.getResource().getContents()) {
                if (p instanceof Problem pb) {
                    System.out.println(pb.getSeverity() + ": " + pb.getLocation() + " - " + pb.getDescription());
                }
            }
        }
        
        return null;
    }

    static String display(Operation op) {
        return op.getName() + " -> " +  display(op.getReturnType()) + "{" + display(op.getBody()) + "}";
    }

    static String display(OclExpression o) {
        return switch (o) {
            case OperatorCallExp op -> display(op.getSource()) + " " + op.getOperationName() + " " + display(op.getArguments().get(0));

            case OperationCallExp op -> display(op.getSource()) + "." + op.getOperationName() +"()";

            case NavigationOrAttributeCallExp n -> display(n.getSource()) + "." + n.getName();
        
            case VariableExp v -> v.getReferredVariable().getVarName();

            case IntegerExp i -> ""+ i.getIntegerSymbol();
            case BooleanType t -> "Boolean";
            default ->
                throw new UnsupportedOperationException("don't support " + o);
        };
    }


    static void printCages(Park p){
        System.out.println("Cages #######");
        for (Cage c : p.getCages()){
            System.out.println("##### "+c.getName()+", capacity: "+c.getAnimals().size()+"/"+c.getCapacity());
            for(Animal a : c.getAnimals()){
                System.out.println(a.getName()+" : "+a.getSpec().getName());
            }
            System.out.println("#####################");
        }
    }



    //ECore <-> Choco: OCL Environement
    public static void main(String[] args) {
        //Make a Zoo Instance with ZooBuilder
        ZooBuilder zooBuilder = new ZooBuilder();
        // zooBuilder.makezoofile0(); // 2cages 3 lions 2 gnou
        // zooBuilder.makezoofile1(6); //3cages 3 lions 2 gnou n capybara                               //5->3:39 then n=6 in less time (like 26s)?!?!
        // zooBuilder.makezoofile2(8); //3cages 3 lions 2 gnou n capybara, a lion and a gnou in a cage   //8->1s, 9->54s

        //Load a Zoo Instance
        Park p2 = (Park) loadInstance("myZoo.xmi");

        System.out.println("Animals ####");
        for(Animal a : p2.getAnimals()){
            String cagestat=" in a cage";
            if(a.getCage()==null) cagestat="";
            System.out.println(a.getName()+" : "+a.getSpec().getName()+cagestat);
        }
        printCages(p2);

        //Load Zoo Constraints 
        var m = App.loadModule("model/zoo.atl");
        System.out.println(m.getName());
        for (var e : m.getElements()) {
            if (e instanceof Helper h) {
                Operation op = (Operation)h.getDefinition().getFeature();
                System.out.println(App.display(op));
            }
        }


        // Here we make our Orders of Objects, List<> provides indexOf
        List<Animal> csp_animals = p2.getAnimals(); 
        List<Cage> csp_cages = p2.getCages();
        List<Species> csp_species = p2.getSpecs();

        //Build and Solve CSP
        
        // IntVar[][] problemVars = cage2animal_LinkVars.toArray(new IntVar[cage2animal_LinkVars.size()][]);
        // m.getSolver().setSearch(Search.intVarSearch(ArrayUtils.flatten(problemVars)));
        // System.out.println("Solving");
        // Solution solution = m.getSolver().findSolution();
        // if(solution != null){
        //     System.out.println(solution.toString());
        //     for(var c:csp_cages){
        //         int maxCard = c.eClass().getEReferences().getFirst().getUpperBound();
        //         int[] values = new int[maxCard];
        //         IntVar[] linkVar = cage2animal.get(c);
        //         for(int i=0;i<maxCard;i++){
        //             values[i] = linkVar[i].getValue();
        //             // System.out.println(values[i]);
        //             if(values[i]!=cA) zooBuilder.putInCage(csp_animals.get(values[i]), c);
        //         }
        //     }
        // }



        // //OutPut
        // System.out.println("#######################");
        // System.out.println("Zoo Config ############");
        // printCages(p2);
        // saveInstance("myZooConfig.xmi", p2);
    }
}