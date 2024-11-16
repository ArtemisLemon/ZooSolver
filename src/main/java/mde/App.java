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
import java.lang.ref.Reference;
import java.nio.file.Path;

import org.oclinchoco.CSP;
import org.oclinchoco.ReferenceTable;

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

    static void saveInstance(String path, Park park){
        ResourceSetImpl rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        rs.getPackageRegistry().put(ZooPackage.eNS_URI,ZooPackage.eINSTANCE);
        Resource res = rs.createResource(URI.createFileURI(path));
        res.getContents().add(park);
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
        Park park = (Park) loadInstance("myZoo.xmi");

        System.out.println("Animals ####");
        for(Animal a : park.getAnimals()){
            String cagestat=" in a cage";
            if(a.getCage()==null) cagestat="";
            System.out.println(a.getName()+" : "+a.getSpec().getName()+cagestat);
        }
        printCages(park);

        //Load Zoo Constraints 
        var atl = App.loadModule("model/zoo.atl");
        System.out.println(atl.getName());
        for (var e : atl.getElements()) {
            if (e instanceof Helper h) {
                Operation op = (Operation)h.getDefinition().getFeature();
                System.out.println(h.getDefinition().getContext_().getContext_().getName()+ " : " + App.display(op));
            }
        }

        // Here we make our Orders of Objects, List<> provides indexOf
        List<Animal> animals = park.getAnimals(); 
        List<Cage> cages = park.getCages();
        List<Species> species = park.getSpecs();

        //Build and Solve CSP
        CSP m = new CSP();

        // Get cardinalities from metamodel
        int a2m=1, a2M=1, c2m=0,c2M=10;
        ReferenceTable a2c = new ReferenceTable(m,animals.size(),a2M,a2m,cages.size());
        ReferenceTable c2a = new ReferenceTable(m,cages.size(),c2M,c2m,animals.size());
        ReferenceTable a2s = new ReferenceTable(m,animals.size(),a2M,a2m,species.size());
        // reftable.loadData(int[][]);
        // ReferenceTable.Opposites(a2c,c2a);
        // c2a.applyContainment();
        java.util.Hashtable<String, ReferenceTable> reftables = new Hashtable<>();
        reftables.put("cage",a2c);
        reftables.put("animals",c2a);
        reftables.put("spec",a2s);


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
        m.model().getSolver().printStatistics();
        // System.out.println("#######################");
        // System.out.println("Zoo Config ############");
        // printCages(park);
        // saveInstance("myZooConfig.xmi", park);
    }
}