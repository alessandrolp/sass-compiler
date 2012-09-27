package com.vaadin.sass.visitor;

import java.util.HashSet;
import java.util.regex.Pattern;

import com.vaadin.sass.tree.BlockNode;
import com.vaadin.sass.tree.Node;
import com.vaadin.sass.tree.SimpleNode;
import com.vaadin.sass.tree.controldirective.EachDefNode;

public class EachVisitor implements Visitor {

    HashSet<EachDefNode> controlDefs = new HashSet<EachDefNode>();
    private Node rootNode;

    @Override
    public void traverse(Node node) throws Exception {
        rootNode = node;
        for (Node child : node.getChildren()) {
            if (child instanceof EachDefNode) {
                controlDefs.add((EachDefNode) child);
            }
        }

        replaceControlNodes();

    }

    private void replaceControlNodes() {
        for (final EachDefNode defNode : controlDefs) {
            replaceEachDefNode(defNode);

        }
    }

    private void replaceEachDefNode(EachDefNode defNode) {
        for (final Node child : defNode.getChildren()) {
            if (child instanceof BlockNode) {
                Node lastNode = defNode;
                for (final String variable : defNode.getVariables()) {

                    String output = child.toString();
                    output = output.replaceAll(
                            Pattern.quote("#{" + defNode.getVariableName()
                                    + "}"), variable);
                    SimpleNode simple = new SimpleNode(output);

                    rootNode.appendChild(simple, lastNode);
                    lastNode = simple;
                }
            }
        }
        rootNode.removeChild(defNode);
    }

}