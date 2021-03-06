package org.jboss.as.console.client.widgets.forms;

import org.jboss.dmr.client.ModelNode;

import java.util.LinkedList;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * Represents entity address meta data, that declared using the {@link Address} annotation.<br/>
 * Address declarations may contain wildcards. An AddressBinding supports replacement of
 * wildcards with proper address values when turned into {@link ModelNode} representations.
 *
 * @author Heiko Braun
 * @date 9/23/11
 */
public class AddressBinding {

    private List<String[]> address = new LinkedList<String[]>();
    private int countedWildcards = -1;

    public AddressBinding() {
    }

    public void add(String parent, String child)
    {
        address.add(new String[]{parent, child});
    }

    public int getNumWildCards() {

        if(countedWildcards <0)
        {
            int counter = 0;

            for(String[] tuple : address)
            {
                if(tuple[0].startsWith("{"))
                    counter++;

                if(tuple[1].startsWith("{"))
                    counter++;
            }

            countedWildcards = counter;
        }

        return countedWildcards;
    }

    /**
     * Turns this address into a ModelNode with an address property.
     *
     * @param args parameters for address wildcards
     * @return a ModelNode with an address property
     */
    public ModelNode asResource(String... args) {
        return asResource(new ModelNode(), args);
    }

    /**
     * Turns this address into a ModelNode with an address property.<br/>
     * This method allows to specify a base address prefix (i.e server vs. domain addressing).
     *
     * @param baseAddress
     * @param args parameters for address wildcards
     * @return a ModelNode with an address property
     */
    public ModelNode asResource(ModelNode baseAddress, String... args) {

        assert getNumWildCards() ==args.length :
                "Address arguments don't match number of wildcards: "+args.length+","+getNumWildCards();

        ModelNode model = new ModelNode();
        model.get(ADDRESS).set(baseAddress);

        int argsCounter = 0;
        for(String[] tuple : address)
        {
            String parent = tuple[0];
            String child = tuple[1];

            if(parent.startsWith("{"))
            {
                parent = args[argsCounter];
                argsCounter++;
            }

            if(child.startsWith("{"))
            {
                child = args[argsCounter];
                argsCounter++;
            }

            model.get(ADDRESS).add(parent, child);
        }

        return model;
    }

    /**
     * Turns this address into a subresource address,
     * including the address and child-type properties.

     *
     * @param args parameters for address wildcards
     * @return  ModelNode including address and child-type property
     */
    public ModelNode asSubresource(String... args) {
        return asSubresource(new ModelNode(), args);
    }

    /**
     * Turns this address into a subresource address,
     * including the address and child-type properties.<br/>
     * The child-type is derived from the last address token qualifier.
     *
     * This method allows to specify a base address prefix (i.e server vs. domain addressing).
     *
     * @param baseAddress
     * @param args parameters for address wildcards
     * @return  ModelNode including address and child-type property
     */
    public ModelNode asSubresource(ModelNode baseAddress, String... args) {

        int numWildCards = getNumWildCards();
        int wildcards = (numWildCards - 1) > 0 ? numWildCards-1 : 0;

        assert wildcards == args.length :
                "Address arguments don't match number of wildcards: "+args.length+","+wildcards;

        ModelNode model = new ModelNode();
        model.get(ADDRESS).set(baseAddress);

        int argsCounter = 0;

        for(int i=0; i<address.size()-1; i++)
        {
            String[] tuple = address.get(i);

            String parent = tuple[0];
            String child = tuple[1];

            if(parent.startsWith("{"))
            {
                parent = args[argsCounter];
                argsCounter++;
            }

            if(child.startsWith("{"))
            {
                child = args[argsCounter];
                argsCounter++;
            }

            model.get(ADDRESS).add(parent, child);
        }

        String[] lastTuple = address.get(address.size()-1);
        String childType = lastTuple[0];

        if(childType.startsWith("{"))
        {
            childType = args[argsCounter];
            argsCounter++;
        }

        model.get(CHILD_TYPE).set(childType);

        return model;

    }
}
