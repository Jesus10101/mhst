import java.util.Scanner;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MHSEC
{
    //====================Variables====================
    private byte[] buffer;

    // Item Box
    private final List<Integer> item_offsets = new ArrayList<>();
    private final List<Integer> item_ids = new ArrayList<>();
    private final List<String> item_names = new ArrayList<>();

    private final List<String> monster_names = new ArrayList<>();
    private final List<Integer> monster_levels = new ArrayList<>();
    private final List<Integer> monster_offsets = new ArrayList<>();
    //====================Constants====================
    private final static String MAPPING_FILE_NAME = "idmap.txt";

    // Monster
    private final static int OFFSETA_MONSTER = 0xA150;
    private final static int OFFSETR_MONSTER_SLOT = 0x424;
    private final static int SIZE_MONSTER_SLOT = 0x4;
    private final static int MONSTER_SLOT_AVAILABLE = 0x1;
    private final static int MONSTER_SLOT_UNAVAILABLE = 0x2;
    private final static int OFFSETR_MONSTER_EXP = 0xE0;
    private final static int LIMIT_MONSTER_EXP = 0xFFFFFF;
    private final static int OFFSETR_MONSTER_AIV = 0xD8;
    private final static int OFFSETR_MONSTER_HIV = 0xD9;
    private final static int OFFSETR_MONSTER_DIV = 0xDA;
    private final static int OFFSETR_MONSTER_LEVEL = 0x5C;
    private final static int OFFSETR_MONSTER_NAME = 0;
    private final static int SIZE_MONSTER = 0x478;
    // you can catch 237 monsters max
    private final static int OFFSETA_MONSTE_END = 0x4786F;
    // Character
    private final static int OFFSETA_CHARACTER = 0x9DA0;
    private final static int OFFSETR_CHARACTER_NAME = 0x0;
    private final static int OFFSETA_CHARACTER_EXP = 0x9E68;
    private final static int LIMIT_CHARACTER_EXP = 25165822;
    // Misc
    // Uint32_t
    // Max 9999999
    private final static int OFFSETA_MONEY = 0x5B404;
    private final static int LIMIT_MONEY = 9999999;

    // ITEM BOX
    private final static int OFFSETA_ITEM_BOX = 0x10;
    private final static int SIZE_ITEM = 0x8;
    private final static int OFFSETR_ITEM_ID = 0x0;
    private final static int OFFSETR_ITEM_COUNT = 0x2;
    private final static int OFFSETA_ITEM_BOX_END = 0x2EE7;
    private final static int FIRST_KEY_ITEM_ID = 1227;

    // Egg Fragments
    private final static int OFFSETA_EGG_FRAGMENTS = 0x9790;
    private final static int SIZE_EGG_FRAGMENT = 0xC;

    private static int byte_to_uint(byte b)
    {
        return (int) (b) & 0xFF;
    }

    private static int byte_to_uint16_le(byte[] ref, int offset)
    {
        if (ref.length < offset + 2)
            throw new RuntimeException("Buffer overflowed - Offset " + offset);
        return byte_to_uint(ref[offset]) | (byte_to_uint(ref[offset + 1]) << 8);
    }

    private static void write_uint16_le(byte[] ref, int offset, int val)
    {
        if (ref.length < offset + 2)
            throw new RuntimeException("Buffer overflowed - Offset " + offset);
        ref[offset] = (byte) (val & 0xFF);
        ref[offset + 1] = (byte) ((val >> 8) & 0xFF);
    }

    private static int byte_to_uint32_le(byte[] ref, int offset)
    {
        if (ref.length < offset + 4)
            throw new RuntimeException("Buffer overflowed - Offset " + offset);
        return byte_to_uint(ref[offset]) | (byte_to_uint(ref[offset + 1]) << 8) | (byte_to_uint(ref[offset + 2]) << 16) | (byte_to_uint(ref[offset + 3]) << 24);
    }

    private static void write_uint32_le(byte[] ref, int offset, int val)
    {
        if (ref.length < offset + 4)
            throw new RuntimeException("Buffer overflowed - Offset " + offset);
        ref[offset] = (byte) (val & 0xFF);
        ref[offset + 1] = (byte) ((val >> 8) & 0xFF);
        ref[offset + 2] = (byte) ((val >> 16) & 0xFF);
        ref[offset + 3] = (byte) ((val >> 24) & 0xFF);
    }

    private static String read_unicode_null_term(byte[] ref, int offset)
    {
        final StringBuilder name = new StringBuilder();
        for(int i = offset; i < ref.length; i+=2)
        {
            final int each_char = byte_to_uint16_le(ref, i);
            if(each_char == 0)
                break;
            name.append(Character.toChars(each_char));
        }
        return name.toString();
    }

    private void process_monsters()
    {
        for(int i = OFFSETA_MONSTER; i <= OFFSETA_MONSTE_END; i+= SIZE_MONSTER)
        {
            if(byte_to_uint16_le(buffer,i + OFFSETR_MONSTER_NAME) == 0)
            {
                continue;
            }
            monster_levels.add(byte_to_uint(buffer[i+ OFFSETR_MONSTER_LEVEL]));

            monster_names.add(read_unicode_null_term(buffer, i));
            monster_offsets.add(i);
        }
        System.out.println("Processed " + monster_levels.size() + " monsters.");
    }

    private void read_mapping(byte[] mapping) throws IOException
    {
        final BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(mapping), Charset.forName("UTF-16")));
        String line = null;
        int line_num = 0;
        while ((line = in.readLine()) != null)
        {
            line_num++;
            final String[] each_line = line.split("\\t");
            if (each_line.length != 3)
            {
                throw new RuntimeException("Mapping file line #" + line_num + " \"" + line + "\" is corrupted.\n");
            }
            item_offsets.add(Integer.parseInt(each_line[0], 16));
            item_ids.add(Integer.parseInt(each_line[1], 16));
            item_names.add(each_line[2]);
        }
        in.close();
    }

    public MHSEC(byte[] buffer, byte[] mapping) throws IOException
    {
        this.buffer = buffer;
        read_mapping(mapping);
        System.out.println("Character name: \"" + read_unicode_null_term(buffer, OFFSETA_CHARACTER + OFFSETR_CHARACTER_NAME) + "\"");
        process_monsters();
    }

    private static String prompt_for_input(String[] args)
    {
        final Map<String, String> mapping = new HashMap<>();
        final Scanner scanner = new Scanner(System.in);
        String line = null;
        if ((args.length & 1) != 0)
        {
            throw new RuntimeException("Prompt arguments must be multiple of 2.");
        }
        for (int i = 0; i < args.length; i += 2)
        {
            mapping.put(args[i], args[i + 1]);
            System.out.println("\"" + args[i] + "\" - " + args[i + 1]);
        }

        while (true)
        {
            System.out.print(">");
            line = scanner.next();
            final String val = mapping.get(line);
            if (val == null)
            {
                System.out.println("Invalid selection, please select again.");
            } else
            {
                break;
            }
        }
        return line;
    }

    // MAIN LOOP CMD CONSTANTS
    private final static String CMD_MON_EDIT = "m";
    private final static String CMD_CHAR_EDIT = "c";
    private final static String CMD_ITEM_EDIT = "i";
    private final static String CMD_EGG_F_EDIT = "e";
    private final static String CMD_MISC_EDIT = "o";
    private final static String CMD_EXIT = "x";
    private final static String CMD_BACK = "b";

    public void run()
    {
        while (true)
        {
            boolean end = false;
            // TODO: SERIOUSLY JAVA???? NO FUNCTION POINTER????
            String[] prompt = {CMD_MON_EDIT, "Monster Editing",
                    CMD_CHAR_EDIT, "Character Editing",
                    CMD_ITEM_EDIT, "Item Editing",
                    CMD_EGG_F_EDIT, "Egg Fragment Editing",
                    CMD_MISC_EDIT, "Misc Editing",
                    CMD_EXIT, "Save and Exit"};
            String cmd = prompt_for_input(prompt);
            switch (cmd)
            {
                case CMD_MON_EDIT:
                    monster_set();
                    break;
                case CMD_CHAR_EDIT:
                    character_set();
                    break;
                case CMD_ITEM_EDIT:
                    item_set();
                    break;
                case CMD_EGG_F_EDIT:
                    egg_f_set();
                    break;
                case CMD_MISC_EDIT:
                    misc_set();
                    break;
                case CMD_EXIT:
                    end = true;
                    break;
                default:
                    throw new RuntimeException("Command system is corrupted...");
            }
            if (end)
            {
                break;
            }
        }
    }

    private final static String CMD_SET_ALL = "a";
    private final static String CMD_SET_EXISITING = "e";

    private void item_set()
    {
        while (true)
        {
            boolean end = false;
            String[] prompt = {CMD_SET_ALL, "Give 986x all items",
                    CMD_SET_EXISITING, "Set 999x existing items",
                    CMD_BACK, "Go Back"};
            String cmd = prompt_for_input(prompt);
            switch (cmd)
            {
                case CMD_SET_ALL:
                    for (int i = 0; i < item_offsets.size(); i++)
                    {
                        write_uint16_le(buffer, item_offsets.get(i) + OFFSETR_ITEM_ID, item_ids.get(i));
                        write_uint16_le(buffer, item_offsets.get(i) + OFFSETR_ITEM_COUNT, 986);
                    }
                    System.out.println("Succeeded.");
                    break;
                case CMD_SET_EXISITING:
                    for (int i = OFFSETA_ITEM_BOX; i < OFFSETA_ITEM_BOX_END; i += SIZE_ITEM)
                    {
                        final int item_id = byte_to_uint16_le(buffer, i + OFFSETR_ITEM_ID);

                        if (item_id == FIRST_KEY_ITEM_ID)
                        {
                            break;
                        }
                        if (item_id != 0)
                        {
                            write_uint16_le(buffer, i + OFFSETR_ITEM_COUNT, 999);
                        }
                    }
                    System.out.println("Succeeded.");
                    break;
                case CMD_BACK:
                    end = true;
                    break;
                default:
                    throw new RuntimeException("Command system is corrupted...");
            }
            if (end)
            {
                break;
            }
        }
    }

    private final static String CMD_MON_EXP= "e";
    private final static String CMD_MON_IV = "i";
    private final static String CMD_MON_SLOTS = "s";

    private void monster_set_with_cmd(String action)
    {
        while (true)
        {
            System.out.println("Select a monster.");
            String[] prompt = new String[monster_names.size()*2+2];
            for(int i = 0; i < monster_names.size(); i++)
            {
                prompt[2*i] = Integer.toString(i);
                prompt[2*i+1] = monster_names.get(i) + " [Lv " + monster_levels.get(i) + "]" + Integer.toHexString(monster_offsets.get(i));
            }
            prompt[prompt.length - 1] = "Go Back";
            prompt[prompt.length - 2] = CMD_BACK;

            String cmd = prompt_for_input(prompt);
            if(cmd.equals(CMD_BACK))
            {
                break;
            }

            int monster_offset = monster_offsets.get(Integer.parseInt(cmd));

            switch (action)
            {
                case CMD_MON_EXP:
                    write_uint32_le(buffer, monster_offset + OFFSETR_MONSTER_EXP, LIMIT_MONSTER_EXP);
                    System.out.println("Succeeded.");
                    break;
                case CMD_MON_IV:
                    buffer[monster_offset + OFFSETR_MONSTER_AIV] = (byte)0xFF;
                    buffer[monster_offset + OFFSETR_MONSTER_HIV] = (byte)0xFF;
                    buffer[monster_offset + OFFSETR_MONSTER_DIV] = (byte)0xFF;
                    System.out.println("Succeeded.");
                    break;
                case CMD_MON_SLOTS:
                    for(int i = 0; i < 9; i++)
                    {
                        if(byte_to_uint32_le(buffer, monster_offset + OFFSETR_MONSTER_SLOT + i * SIZE_MONSTER_SLOT) == MONSTER_SLOT_UNAVAILABLE)
                        {
                            write_uint32_le(buffer, monster_offset + OFFSETR_MONSTER_SLOT + i * SIZE_MONSTER_SLOT, MONSTER_SLOT_AVAILABLE);
                        }
                    }
                    System.out.println("Succeeded.");
                    break;
                default:
                    throw new RuntimeException("Command system is corrupted...");
            }
            break;
        }
    }

    private void monster_set()
    {
        while (true)
        {
            String prompt[] = {CMD_MON_EXP, "Set level to 99",
                    CMD_MON_IV, "Set IV to max (very op)",
                    CMD_MON_SLOTS, "Set to 9 skill slots (open unopened slots)",
                    CMD_BACK, "Go Back"};

            String cmd = prompt_for_input(prompt);

            if(cmd.equals(CMD_BACK))
            {
                break;
            }

            monster_set_with_cmd(cmd);
        }
    }

    private final static String CMD_SET_MONEY = "m";
    private void misc_set()
    {
        while (true)
        {
            boolean end = false;
            String[] prompt = {CMD_SET_MONEY, "Max money",
                    CMD_BACK, "Go Back"};
            String cmd = prompt_for_input(prompt);
            switch (cmd)
            {
                case CMD_SET_MONEY:
                    write_uint32_le(buffer, OFFSETA_MONEY, LIMIT_MONEY);
                    System.out.println("Succeeded.");
                    break;
                case CMD_BACK:
                    end = true;
                    break;
                default:
                    throw new RuntimeException("Command system is corrupted...");
            }
            if (end)
            {
                break;
            }
        }
    }

    private final static String CMD_SET_CHAR_EXP = "m";
    private void character_set()
    {
        while (true)
        {
            boolean end = false;
            String[] prompt = {CMD_SET_CHAR_EXP, "Max character exp.",
                    CMD_BACK, "Go Back"};
            String cmd = prompt_for_input(prompt);
            switch (cmd)
            {
                case CMD_SET_CHAR_EXP:
                    write_uint32_le(buffer, OFFSETA_CHARACTER_EXP, LIMIT_CHARACTER_EXP);
                    System.out.println("Succeeded.");
                    break;
                case CMD_BACK:
                    end = true;
                    break;
                default:
                    throw new RuntimeException("Command system is corrupted...");
            }
            if (end)
            {
                break;
            }
        }
    }

    private final static String CMD_GIVE_DINO = "d";
    private final static byte[] EGG_F_DINO = {0x08, 0x00, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x08, 0x01, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x08, 0x02, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x08, 0x03, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x08, 0x04, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x08, 0x05, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x08, 0x07, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x08, 0x08, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    private void egg_f_set()
    {
        while (true)
        {
            boolean end = false;
            String[] prompt = {CMD_GIVE_DINO, "Give Dinovaldo (will overwrite existing egg fragments)",
                    CMD_BACK, "Go Back"};
            String cmd = prompt_for_input(prompt);
            switch (cmd)
            {
                case CMD_GIVE_DINO:
                    System.arraycopy(EGG_F_DINO, 0, buffer, OFFSETA_EGG_FRAGMENTS, EGG_F_DINO.length);
                    System.out.println("Succeeded. Please combine the egg fragments.");
                    break;
                case CMD_BACK:
                    end = true;
                    break;
                default:
                    throw new RuntimeException("Command system is corrupted...");
            }
            if (end)
            {
                break;
            }
        }
    }


    public static void main(String[] args) throws IOException
    {
        if (args.length == 1)
        {
            final byte[] buffer;
            final byte[] mapping;
            final Path path = Paths.get(args[0]);
            final Path output = Paths.get(args[0] + ".hacked");
            final Path mpath = Paths.get(MAPPING_FILE_NAME);


            buffer = Files.readAllBytes(path);
            System.out.print("Read save file \"" + args[0] + "\". Size: " + buffer.length + " bytes.\n");
            mapping = Files.readAllBytes(mpath);
            System.out.print("Read mapping file \"" + MAPPING_FILE_NAME + "\". Size: " + mapping.length + ".\n");

            final MHSEC sec = new MHSEC(buffer, mapping);
            sec.run();
            Files.write(output, buffer);
            System.out.print("Completed.\n");
        } else
        {
            System.err.println("Incorret arguments.");
        }
    }
}
