package handling.handlers;

import client.Client;
import client.character.Char;
import client.character.commands.AdminCommand;
import client.character.commands.AdminCommands;
import client.character.commands.Command;
import client.character.items.Item;
import client.character.skills.AttackInfo;
import client.character.skills.MobAttackInfo;
import client.field.Field;
import client.field.Portal;
import connection.InPacket;
import constants.SkillConstants;
import enums.InvType;
import loaders.FieldData;
import packet.Stage;
import packet.WvsContext;

import java.lang.reflect.InvocationTargetException;

import static enums.InvType.*;

/**
 * Created on 12/14/2017.
 */
public class WorldHandler {
    public static void handleCharLogin(Client c, InPacket inPacket) {
        int worldId = inPacket.decodeInt();
        int charId = inPacket.decodeInt(); // does not properly get sent to us?
        Char chr = Char.getFromDBById(charId);
        chr.setClient(c);
        c.setChr(chr);
        Field field = new Field(chr.getFieldID() <= 0 ? 100000000 : chr.getFieldID(), c.getChannel());
        c.write(WvsContext.updateEventNameTag(new int[]{}));
        c.write(Stage.setField(chr, field, c.getChannel(), false, 0, true, false,
                (byte) 0, false, 100, null, true, -1));


    }

    public static void handleChat(Client c, InPacket inPacket) {
        inPacket.decodeInt();
        String msg = inPacket.decodeString();
        if (msg.length() > 0 && msg.charAt(0) == '@') {
            if (msg.equalsIgnoreCase("@check")) {
                WvsContext.dipose(c, c.getChr());
            }
        } else if (msg.charAt(0) == AdminCommand.getPrefix()) {
            for (Class clazz : AdminCommands.class.getClasses()) {
                try {
                    Command o = (Command) clazz.getConstructor().newInstance();
                    String[] split = msg.split(" ");
                    String[] fin = new String[split.length - 1];
                    System.arraycopy(split, 2, fin, 1, fin.length - 1);
                    o.execute(fin);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void handleInventoryOperation(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        inPacket.decodeInt(); // update tick
        InvType invType = InvType.getInvTypeByVal(inPacket.decodeByte());
        short oldPos = inPacket.decodeShort();
        short newPos = inPacket.decodeShort();
        short quantity = inPacket.decodeShort();
        Item item = chr.getInventoryByInvType(invType).getItemBySlot(oldPos);
        if(item == null) {
            item = chr.getInventoryByInvType(EQUIPPED).getItemBySlot(oldPos);
            if (item == null) {
                return; // needs more null checks
            }
        }
        item.setBagIndex(newPos);
        if(invType == EQUIP) {
            InvType invTypeFrom = oldPos < 0 ? EQUIPPED : EQUIP;
            InvType invTypeTo = newPos < 0 ? EQUIPPED : EQUIP;
            chr.getInventoryByInvType(invTypeFrom).removeItem(item);
            chr.getInventoryByInvType(invTypeTo).addItem(item);
        }
         // TODO dropping items
        c.write(WvsContext.inventoryOperation(c.getChr(), true, (byte) 2, oldPos, newPos, invType, quantity,
                false, 0, item));


    }

    public static void handleMagicAttack(Client c, InPacket inPacket) {
        AttackInfo attackInfo = new AttackInfo();
        boolean fieldKey = inPacket.decodeByte() == 1;
        byte mask = inPacket.decodeByte();
        byte hits = (byte) (mask & 0xF);
        byte mobCount = (byte) (mask >>> 4);
        int skillId = inPacket.decodeInt();
        byte slv = inPacket.decodeByte();
        inPacket.decodeInt(); // crc
        int keyDown = -1;
        if (SkillConstants.isKeyDownSkill(skillId)) {
            keyDown = inPacket.decodeInt();
        }
        inPacket.decodeByte(); // some zero byte
        byte idk = inPacket.decodeByte();
        short maskie = inPacket.decodeShort();
        boolean left = ((maskie >> 15) & 1) != 0;
        short attackAction = (short) (maskie & 0x7FFF);
        inPacket.decodeInt(); // another crc (GETCRC32Svr<long>(&a[*n], 0x405u))
        byte attackActionType = inPacket.decodeByte();
        byte idk0 = 0;
        if (SkillConstants.isEvanForceSkill(skillId)) {
            idk0 = inPacket.decodeByte();
        }
        byte mask2 = inPacket.decodeByte();
        byte attackSpeed = (byte) (mask2 & 0xFFFF);
        byte reduceCount = (byte) (mask2 >>> 4);
        int psdTargetPlus = inPacket.decodeInt();
        int id = inPacket.decodeInt();
        attackInfo.fieldKey = fieldKey;
        attackInfo.hits = hits;
        attackInfo.mobCount = mobCount;
        attackInfo.skillId = skillId;
        attackInfo.slv = slv;
        attackInfo.keyDown = keyDown;
        attackInfo.idk = idk;
        attackInfo.left = left;
        attackInfo.attackAction = attackAction;
        attackInfo.attackActionType = attackActionType;
        attackInfo.idk0 = idk0;
        attackInfo.attackSpeed = attackSpeed;
        attackInfo.reduceCount = reduceCount;
        attackInfo.psdTargetPlus = psdTargetPlus;
        attackInfo.someId = id;
        for (int i = 0; i < mobCount; i++) {
            MobAttackInfo mai = new MobAttackInfo();
            int mobId = inPacket.decodeInt();
            byte idk1 = inPacket.decodeByte();
            byte idk2 = inPacket.decodeByte();
            byte idk3 = inPacket.decodeByte();
            byte idk4 = inPacket.decodeByte();
            byte idk5 = inPacket.decodeByte();
            int idk6 = inPacket.decodeInt();
            byte calcDamageStatIndex = inPacket.decodeByte();
            short rcDstX = inPacket.decodeShort();
            short rectRight = inPacket.decodeShort();
            short oldPosX = inPacket.decodeShort(); // ?
            short oldPosY = inPacket.decodeShort(); // ?
            byte hpPerc = inPacket.decodeByte();
            int[] damages;
            if (skillId == 80001835) {
                byte size = inPacket.decodeByte();
                damages = new int[size];
                for (int j = 0; j < size; j++) {
                    damages[j] = inPacket.decodeInt();
                }
            } else {
                short size = inPacket.decodeShort();
                damages = new int[size];
                for (int j = 0; j < size; j++) {
                    damages[j] = inPacket.decodeInt();
                }
            }
            // TODO damage here? I thought it was the above thing
            int mobUpDownYRange = inPacket.decodeInt();
            inPacket.decodeInt(); // mob crc
            // Begin PACKETMAKER::MakeAttackInfoPacket
            byte type = inPacket.decodeByte();
            String currentAnimationName = "";
            int animationDeltaL = 0;
            String[] hitPartRunTimes = new String[0];
            if (type == 1) {
                currentAnimationName = inPacket.decodeString();
                animationDeltaL = inPacket.decodeInt();
                int hitPartRunTimesSize = inPacket.decodeInt();
                hitPartRunTimes = new String[hitPartRunTimesSize];
                for (int j = 0; j < hitPartRunTimesSize; j++) {
                    hitPartRunTimes[j] = inPacket.decodeString();
                }
            } else if (type == 2) {
                currentAnimationName = inPacket.decodeString();
                animationDeltaL = inPacket.decodeInt();
            }
            // End PACKETMAKER::MakeAttackInfoPacket
            mai.mobId = mobId;
            mai.idk1 = idk1;
            mai.idk2 = idk2;
            mai.idk3 = idk3;
            mai.idk4 = idk4;
            mai.idk5 = idk5;
            mai.idk6 = idk6;
            mai.calcDamageStatIndex = calcDamageStatIndex;
            mai.rcDstX = rcDstX;
            mai.rectRight = rectRight;
            mai.oldPosX = oldPosX;
            mai.oldPosY = oldPosY;
            mai.hpPerc = hpPerc;
            mai.damages = damages;
            mai.mobUpDownYRange = mobUpDownYRange;
            mai.type = type;
            mai.currentAnimationName = currentAnimationName;
            mai.animationDeltaL = animationDeltaL;
            mai.hitPartRunTimes = hitPartRunTimes;
            attackInfo.mobAttackInfo.add(mai);
        }
        if (skillId > 27111303) {
            if (skillId == 27121052 || skillId == 80001837) {
                int x = inPacket.decodeShort();
                int y = inPacket.decodeShort();
                attackInfo.x = x;
                attackInfo.y = y;
            }
        } else if (skillId != 32111016) {
            short forcedX = inPacket.decodeShort();
            short forcedY = inPacket.decodeShort();
            boolean dragon = inPacket.decodeByte() != 0;
            attackInfo.forcedX = forcedX;
            attackInfo.forcedY = forcedY;
            if(dragon) {
                short rcDstRight = inPacket.decodeShort();
                short rectRight = inPacket.decodeShort();
                short x = inPacket.decodeShort();
                short y = inPacket.decodeShort();
                inPacket.decodeByte(); // always 0
                inPacket.decodeByte(); // -1
                inPacket.decodeByte(); // 0
                attackInfo.rcDstRight = rcDstRight;
                attackInfo.rectRight = rectRight;
                attackInfo.x = x;
                attackInfo.y = y;
            }
            if(skillId == 12100029) {
                int option = inPacket.decodeInt();
                attackInfo.option = option;
            } else {
                switch(skillId) {
                    case 2121003:
                        byte size = inPacket.decodeByte();
                        int[] idkArr = new int[size];
                        for (int i = 0; i < size; i++) {
                            idkArr[i] = inPacket.decodeInt();
                        }
                        attackInfo.idkArr = idkArr;
                        break;
                    case 2111003:
                        boolean force = inPacket.decodeByte() != 0;
                        short forcedXSh = inPacket.decodeShort();
                        short forcedYSh = inPacket.decodeShort();
                        attackInfo.force = force;
                        attackInfo.forcedXSh = forcedXSh;
                        attackInfo.forcedYSh = forcedYSh;
                        break;
                    case 80001835: // unreachable?
                        byte sizeB = inPacket.decodeByte();
                        int[] idkArr2 = new int[sizeB];
                        short[] shortArr2 = new short[sizeB];
                        for (int i = 0; i < sizeB; i++) {
                            idkArr2[i] = inPacket.decodeInt();
                            shortArr2[i] = inPacket.decodeShort();
                        }
                        short delay = inPacket.decodeShort();
                        attackInfo.idkArr = idkArr2;
                        attackInfo.shortArr = shortArr2;
                        attackInfo.delay = delay;
                }
            }
        }
    }

    public static void handleChangeFieldRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        byte idk = inPacket.decodeByte();
        int idk1 = inPacket.decodeInt();
        int x = inPacket.decodeShort();
        int y = inPacket.decodeShort();
        String portalName = inPacket.decodeString();
        int fieldId = chr.getFieldID();
        Field field = FieldData.getFieldById(fieldId);
        Portal portal = field.getPortalByName(portalName);
        Field toField = FieldData.getFieldById(portal.getTargetMapId());
        chr.setFieldID(toField.getId());
        Portal toPortal = toField.getPortalByName(portal.getTargetPortalName());
        c.write(Stage.setField(chr, toField, c.getChannel(), false, 0, false, chr.hasBuffProtector(),
                (byte) toPortal.getId(), false, 100, null, false, -1));
    }
}
