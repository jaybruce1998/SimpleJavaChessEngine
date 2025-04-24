import java.util.ArrayList;
import java.util.List;

public class Attacks {
    public static byte[][][] Bishop = new byte[64][][];
    public static byte[][][] Rook = new byte[64][][];
    public static byte[][][] Queen = new byte[64][][];
    public static byte[][] King = new byte[64][];
    public static byte[][] Knight = new byte[64][];
    public static byte[][] BlackPawn = new byte[64][];
    public static byte[][] WhitePawn = new byte[64][];

    static final int[] DIAGONALS_FILE = {-1, 1, 1, -1};
    static final int[] DIAGONALS_RANK = {-1, -1, 1, 1};

    static final int[] STRAIGHTS_FILE = {-1, 0, 1, 0};
    static final int[] STRAIGHTS_RANK = {-0, -1, 0, 1};

    static final int[] KING_FILE = {-1, 0, 1, 1, 1, 0, -1, -1};
    static final int[] KING_RANK = {-1, -1, -1, 0, 1, 1, 1, 0};

    static final int[] KNIGHT_FILE = {-1, -2, 1, 2, -1, -2, 1, 2};
    static final int[] KNIGHT_RANK = {-2, -1, -2, -1, 2, 1, 2, 1};

    static final List<Byte> IndexBuffer = new ArrayList<>();

    static {
        for (int index = 0; index < 64; index++) {
            int rank = index / 8;
            int file = index % 8;

            Bishop[index] = new byte[4][];
            Rook[index] = new byte[4][];
            Queen[index] = new byte[8][];

            //Add 4 diagonal lines
            for (int dir = 0; dir < 4; dir++)
                Queen[index][dir] = Bishop[index][dir] = WalkTheLine(rank, file, DIAGONALS_RANK[dir], DIAGONALS_FILE[dir]);

            //Add 4 straight lines
            for (int dir = 0; dir < 4; dir++)
                Queen[index][dir+4] = Rook[index][dir] = WalkTheLine(rank, file, STRAIGHTS_RANK[dir], STRAIGHTS_FILE[dir]);

            //Add Knight&King attack patterns
            King[index] = ApplyPattern(rank, file, KING_RANK, KING_FILE);
            Knight[index] = ApplyPattern(rank, file, KNIGHT_RANK, KNIGHT_FILE);

            //Add size-2 arrays for pawn attacks
            BlackPawn[index] = PawnAttacks(rank, file, -1);
            WhitePawn[index] = PawnAttacks(rank, file, +1);
        }
    }

    private static byte[] toArray() {
        byte[] r = new byte[IndexBuffer.size()];
        for (int i = 0; i < r.length; i++)
            r[i] = IndexBuffer.get(i);
        return r;
    }

    private static byte[] PawnAttacks(int rank, int file, int dRank) {
        IndexBuffer.clear();
        TryAddSquare(rank + dRank, file - 1);
        TryAddSquare(rank + dRank, file + 1);
        return toArray();
    }

    private static byte[] ApplyPattern(int rank, int file, int[] patternRank, int[] patternFile) {
        IndexBuffer.clear();
        for (int i = 0; i < 8; i++)
            TryAddSquare(rank + patternRank[i], file + patternFile[i]);
        return toArray();
    }

        private static byte[] WalkTheLine(int rank, int file, int dRank, int dFile)
        {
            IndexBuffer.clear();
            while(true)
            {
                //inc i as long as the resulting index is still on the board
                rank += dRank;
                file += dFile;
                if (!IsLegalSquare(rank, file))
                    break;
                AddSquare(rank, file);
            }
            return toArray();
        }

        private static void TryAddSquare(int rank, int file)
        {
            if (IsLegalSquare(rank, file))
                AddSquare(rank, file);
        }
		
		private static void AddSquare(int rank, int file)
		{
			IndexBuffer.add((byte)(rank*8+file));
		}
		
		private static boolean IsLegalSquare(int rank, int file)
		{
			return rank >= 0 && rank <= 7 && file >= 0 && file <= 7;
		}
}
