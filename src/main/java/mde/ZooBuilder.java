package mde;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.xtext.xbase.lib.Exceptions;

import zoo.Animal;
import zoo.Cage;
import zoo.Park;
import zoo.Species;
import zoo.ZooFactory;
import zoo.ZooPackage;

public class ZooBuilder {

    static ZooFactory z = ZooFactory.eINSTANCE;

    static Park initPark(String name){
        Park out = z.createPark();
        out.setName(name);
        return out;
    }

    static Species makeSpecies(String name, Park park){
        Species out = z.createSpecies();
        out.setName(name);
        park.getSpecs().add(out);
        return out;
    }

    static Cage makeCage(String name, int capacity,Park park){
        Cage out = z.createCage();
        out.setName(name);
        out.setCapacity(capacity);
        park.getCages().add(out);
        return out;
    }

    static Animal makeAnimal(String name, Species species, Park park){
        Animal out = z.createAnimal();
        out.setName(name);
        out.setSpec(species);
        park.getAnimals().add(out);
        return out;
    }

    static void putInCage(Animal animal, Cage cage){
        cage.getAnimals().add(animal);
    }

    public static void makezoofile0() {
        //Make a Zoo
        Park p = initPark("myZoo");
        ResourceSetImpl rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        rs.getPackageRegistry().put(ZooPackage.eNS_URI,ZooPackage.eINSTANCE);
        Resource res = rs.createResource(URI.createFileURI("myZoo.xmi"));
        res.getContents().add(p);

        Species lion = makeSpecies("lion", p);
        Animal lea = makeAnimal("Lea",lion, p);
        Animal leo = makeAnimal("Leo",lion, p);
        Animal lou = makeAnimal("Lou",lion, p);

        Species gnou = makeSpecies("gnou", p);
        Animal gNathan = makeAnimal("gNathan", gnou, p);
        Animal gNatalie = makeAnimal("gNatalie", gnou, p);

        Cage c2 = makeCage("c2", 2,p);
        Cage c4 = makeCage("c4",4,p);
        // Cage c10 = makeCage("c10",10,p);


        try{
            res.save(null);
        } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
        }
    }

    public static void makezoofile1(int n) {
        //Make a Zoo
        Park p = initPark("myZoo");
        ResourceSetImpl rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        rs.getPackageRegistry().put(ZooPackage.eNS_URI,ZooPackage.eINSTANCE);
        Resource res = rs.createResource(URI.createFileURI("myZoo.xmi"));
        res.getContents().add(p);

        Species lion = makeSpecies("lion", p);
        Animal lea = makeAnimal("Lea",lion, p);
        Animal leo = makeAnimal("Leo",lion, p);
        Animal lou = makeAnimal("Lou",lion, p);

        Species gnou = makeSpecies("gnou", p);
        Animal gNathan = makeAnimal("gNathan", gnou, p);
        Animal gNatalie = makeAnimal("gNatalie", gnou, p);

        Species capybara = makeSpecies("capybara", p);
        for(int i=0;i<n;i++){
            makeAnimal("Clem"+i, capybara,p);
        }

        Cage c2 = makeCage("c2", 2,p);
        Cage c4 = makeCage("c4",4,p);
        Cage c10 = makeCage("c10",10,p);


        try{
            res.save(null);
        } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
        }
    }

    public static void makezoofile2(int n) {
        //Make a Zoo
        Park p = initPark("myZoo");
        ResourceSetImpl rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        rs.getPackageRegistry().put(ZooPackage.eNS_URI,ZooPackage.eINSTANCE);
        Resource res = rs.createResource(URI.createFileURI("myZoo.xmi"));
        res.getContents().add(p);

        Species lion = makeSpecies("lion", p);
        Animal lea = makeAnimal("Lea",lion, p);
        Animal leo = makeAnimal("Leo",lion, p);
        Animal lou = makeAnimal("Lou",lion, p);
        

        Species gnou = makeSpecies("gnou", p);
        Animal gNathan = makeAnimal("gNathan", gnou, p);
        Animal gNatalie = makeAnimal("gNatalie", gnou, p);

        Species capybara = makeSpecies("capybara", p);
        for(int i=0;i<n;i++){
            makeAnimal("Clem"+i, capybara,p);
        }

        Cage c2 = makeCage("c2", 2,p);
        Cage c4 = makeCage("c4",4,p);
        Cage c10 = makeCage("c10",10,p);

        putInCage(lea, c4);
        putInCage(gNatalie, c2);

        try{
            res.save(null);
        } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
        }
    }
}
