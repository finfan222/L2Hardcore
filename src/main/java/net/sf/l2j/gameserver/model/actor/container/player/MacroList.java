package net.sf.l2j.gameserver.model.actor.container.player;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.gameserver.enums.ShortcutType;
import net.sf.l2j.gameserver.model.Macro;
import net.sf.l2j.gameserver.model.Macro.MacroCmd;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.SendMacroList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 * An ordered container holding {@link Macro}s of a {@link Player}.
 */
@Slf4j
public class MacroList extends LinkedHashMap<Integer, Macro> {
    private static final long serialVersionUID = 1L;

    private static final String INSERT_MACRO = "REPLACE INTO character_macroses (char_obj_id,id,icon,name,descr,acronym,commands) values(?,?,?,?,?,?,?)";
    private static final String DELETE_MACRO = "DELETE FROM character_macroses WHERE char_obj_id=? AND id=?";
    private static final String LOAD_MACROS = "SELECT char_obj_id, id, icon, name, descr, acronym, commands FROM character_macroses WHERE char_obj_id=?";

    private final Player _owner;

    private int _revision;
    private int _macroId;

    public MacroList(Player owner) {
        _owner = owner;
        _revision = 1;
        _macroId = 1000;
    }

    public int getRevision() {
        return _revision;
    }

    public Macro[] getMacros() {
        return values().toArray(new Macro[size()]);
    }

    /**
     * Add a {@link Macro} to this {@link MacroList}.
     *
     * @param macro : The Macro object to add.
     */
    public void registerMacro(Macro macro) {
        if (macro.id == 0) {
            macro.id = _macroId++;

            while (get(macro.id) != null) {
                macro.id = _macroId++;
            }

            put(macro.id, macro);
        } else {
            final Macro old = put(macro.id, macro);
            if (old != null) {
                deleteMacroFromDb(old);
            }
        }
        registerMacroInDb(macro);
        sendUpdate();
    }

    /**
     * Delete the {@link Macro} corresponding to the id from this {@link MacroList}.
     *
     * @param id : The id of the Macro to delete.
     */
    public void deleteMacro(int id) {
        final Macro toRemove = get(id);
        if (toRemove != null) {
            deleteMacroFromDb(toRemove);
        }

        remove(id);

        // Delete all existing shortcuts refering to this macro id.
        _owner.getShortcutList().deleteShortcuts(id, ShortcutType.MACRO);

        sendUpdate();
    }

    /**
     * Refresh {@link Macro}s list. Used on onEnterWorld.
     */
    public void sendUpdate() {
        _revision++;

        final Macro[] macros = getMacros();
        if (macros.length == 0) {
            _owner.sendPacket(new SendMacroList(_revision, macros.length, null));
        } else {
            for (Macro macro : macros) {
                _owner.sendPacket(new SendMacroList(_revision, macros.length, macro));
            }
        }
    }

    /**
     * Save the given {@link Macro} to the database.
     *
     * @param macro : The Macro to save.
     */
    private void registerMacroInDb(Macro macro) {
        final StringBuilder sb = new StringBuilder(300);
        for (MacroCmd cmd : macro.commands) {
            StringUtil.append(sb, cmd.type, ",", cmd.d1, ",", cmd.d2);
            if (cmd.cmd != null && cmd.cmd.length() > 0) {
                StringUtil.append(sb, ",", cmd.cmd);
            }

            sb.append(';');
        }

        if (sb.length() > 255) {
            sb.setLength(255);
        }

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_MACRO)) {
            ps.setInt(1, _owner.getObjectId());
            ps.setInt(2, macro.id);
            ps.setInt(3, macro.icon);
            ps.setString(4, macro.name);
            ps.setString(5, macro.descr);
            ps.setString(6, macro.acronym);
            ps.setString(7, sb.toString());
            ps.execute();
        } catch (Exception e) {
            log.error("Couldn't store macro.", e);
        }
    }

    /**
     * Delete the given {@link Macro} to the database.
     *
     * @param macro : The Macro to delete.
     */
    private void deleteMacroFromDb(Macro macro) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE_MACRO)) {
            ps.setInt(1, _owner.getObjectId());
            ps.setInt(2, macro.id);
            ps.execute();
        } catch (Exception e) {
            log.error("Couldn't delete macro.", e);
        }
    }

    /**
     * Restore {@link Macro}s associated to the {@link Player} owner.
     */
    public void restore() {
        super.clear();

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(LOAD_MACROS)) {
            ps.setInt(1, _owner.getObjectId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final int id = rs.getInt("id");
                    final int icon = rs.getInt("icon");
                    final String name = rs.getString("name");
                    final String descr = rs.getString("descr");
                    final String acronym = rs.getString("acronym");

                    final List<MacroCmd> commands = new ArrayList<>();
                    final StringTokenizer st1 = new StringTokenizer(rs.getString("commands"), ";");

                    while (st1.hasMoreTokens()) {
                        final StringTokenizer st = new StringTokenizer(st1.nextToken(), ",");
                        if (st.countTokens() < 3) {
                            continue;
                        }

                        final int type = Integer.parseInt(st.nextToken());
                        final int d1 = Integer.parseInt(st.nextToken());
                        final int d2 = Integer.parseInt(st.nextToken());

                        String cmd = "";
                        if (st.hasMoreTokens()) {
                            cmd = st.nextToken();
                        }

                        final MacroCmd mcmd = new MacroCmd(commands.size(), type, d1, d2, cmd);
                        commands.add(mcmd);
                    }

                    final Macro macro = new Macro(id, icon, name, descr, acronym, commands.toArray(new MacroCmd[commands.size()]));
                    put(macro.id, macro);
                }
            }
        } catch (Exception e) {
            log.error("Couldn't load macros.", e);
        }
    }
}