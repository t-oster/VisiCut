/*
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is ICEfaces 1.5 open source software code, released
 * November 5, 2006. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2006 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"
 * License), in which case the provisions of the LGPL License are
 * applicable instead of those above. If you wish to allow use of your
 * version of this file only under the terms of the LGPL License and not to
 * allow others to use your version of this file under the MPL, indicate
 * your decision by deleting the provisions above and replace them with
 * the notice and other provisions required by the LGPL License. If you do
 * not delete the provisions above, a recipient may use your version of
 * this file under either the MPL or the LGPL License."
 *
 */
package org.icepdf.examples.jsf.viewer.view;

import org.icepdf.core.pobjects.OutlineItem;
import org.icepdf.core.pobjects.PageTree;
import org.icepdf.core.pobjects.Destination;
import org.icepdf.core.pobjects.actions.Action;
import org.icepdf.core.pobjects.actions.GoToAction;
import org.icepdf.core.util.Library;

import javax.swing.tree.DefaultMutableTreeNode;

import com.icesoft.faces.component.tree.IceUserObject;

import java.util.Hashtable;

/**
 * PDF document outline which can be used by the ice:tree component.
 * 
 * @since 3.0
 */
public class OutlineItemTreeNode extends DefaultMutableTreeNode {

    private OutlineItem item;
    private boolean loadedChildren;
    private PageTree pageTree;

    /**
     * Creates a new instance of an OutlineItemTreeNode
     *
     * @param item Contains PDF Outline item data
     */
    public OutlineItemTreeNode(PageTree pageTree, OutlineItem item) {
        super();
        this.item = item;
        loadedChildren = false;
        this.pageTree = pageTree;

        // build the tree
        NodeUserObject tmp = new NodeUserObject(this.pageTree, this);
        // set callback
        setUserObject(tmp);
    }

    public OutlineItem getOutlineItem() {
        return item;
    }

    public void recursivelyClearOutlineItems() {
        item = null;
        if (loadedChildren) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                OutlineItemTreeNode node = (OutlineItemTreeNode) getChildAt(i);
                node.recursivelyClearOutlineItems();
            }
        }
    }

    public int getChildCount() {
        ensureChildrenLoaded();
        return super.getChildCount();
    }

    /**
     * Only load children as needed, so don't have to load
     * OutlineItems that the user has not even browsed to
     */
    private void ensureChildrenLoaded() {
        if (!loadedChildren) {
            loadedChildren = true;

            int count = item.getSubItemCount();
            for (int i = 0; i < count; i++) {
                OutlineItem child = item.getSubItem(i);
                OutlineItemTreeNode childTreeNode =
                        new OutlineItemTreeNode(pageTree, child);
                add(childTreeNode);
            }
        }
    }

    public class NodeUserObject extends IceUserObject {

        private int goToPage;

        public NodeUserObject(PageTree pageTree, OutlineItemTreeNode outlineItemTreeNode){
            super(outlineItemTreeNode);

            // append the destination page number
            if (outlineItemTreeNode.getOutlineItem().getDest() != null) {
                goToPage = pageTree.getPageNumber(
                        outlineItemTreeNode.getOutlineItem().getDest()
                                .getPageReference());
            }
            else if (outlineItemTreeNode.getOutlineItem().getAction() != null) {
                OutlineItem item  = outlineItemTreeNode.getOutlineItem();
                Destination dest;
                if (item.getAction() != null) {
                    Action action = item.getAction();
                    if (action instanceof GoToAction) {
                        dest = ((GoToAction) action).getDestination();
                    }  else {
                        Library library = action.getLibrary();
                        Hashtable entries = action.getEntries();
                        dest = new Destination(library, library.getObject(entries, "D"));
                    }
                    goToPage = pageTree.getPageNumber(dest.getPageReference());
                }
            }

            // set title
            setText(outlineItemTreeNode.getOutlineItem().getTitle());

            // setup not state.
            setLeafIcon("tree_document.gif");
            setBranchContractedIcon("tree_document.gif");
            setBranchExpandedIcon("tree_document.gif");
            
            // is item a node or a leaf.
            if (outlineItemTreeNode.getOutlineItem().getSubItemCount() > 0){
                setLeaf(false);
            }
            else{
                setLeaf(true);
            }

        }

        public int getGoToPage() {
            return goToPage;
        }

        public void setGoToPage(int goToPage) {
            this.goToPage = goToPage;
        }
    }

}
