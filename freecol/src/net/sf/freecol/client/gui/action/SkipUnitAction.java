/**
 *  Copyright (C) 2002-2019   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Unit;


/**
 * An action for skipping the active unit.
 */
public class SkipUnitAction extends UnitAction {

    public static final String id = "skipUnitAction";


    /**
     * Creates this action.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public SkipUnitAction(FreeColClient freeColClient) {
        super(freeColClient, id);

        addImageIcons("done");
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final Unit unit = getGUI().getActiveUnit();
        if (unit == null) return;
        if (unit.getState() != Unit.UnitState.SKIPPED) {
            igc().changeState(unit, Unit.UnitState.SKIPPED);
        }
        if (unit.getState() == Unit.UnitState.SKIPPED) {
            igc().nextActiveUnit();
        }
    }
}