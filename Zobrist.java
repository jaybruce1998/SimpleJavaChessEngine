import java.nio.ByteBuffer;
import java.util.Random;

public class Zobrist
{
	static long[][] BoardTable = new long[64][];
	static long[] EnPassantTable = new long[64];
	static long[] CastlingTable = new long[16]; //all permutations of castling rights, CastlingRights.All == 15
	static long Black;
	static long White;

	static
	{
		Random rnd = new Random(228126);
		for (int square = 0; square < 64; square++)
		{
			//6 black pieces + 6 white pieces
			BoardTable[square] = new long[12];
			for (int piece = 0; piece < 12; piece++)
				BoardTable[square][piece] = RandomUInt64(rnd);
			//En passant
			EnPassantTable[square] = RandomUInt64(rnd);
		}
		//Side to Move
		Black = RandomUInt64(rnd);
		White = RandomUInt64(rnd);
		//Castling
		for (int i = 0; i < 16; i++)
			CastlingTable[i] = RandomUInt64(rnd);
	}

	public static long PieceSquare(Piece piece, int square)
	{
		return (piece != Piece.None) ? BoardTable[square][PieceIndex(piece)] : 0;
	}

	public static int PieceIndex(Piece piece)
	{
		return (piece.value >> 1) - 2;
	}

	public static long Castling(byte castlingRights)
	{
		return CastlingTable[castlingRights];
	}

	public static long EnPassant(int square)
	{
		return (square >= 0) ? EnPassantTable[square] : 0;
	}

	public static long SideToMove(Color sideToMove)
	{
		return sideToMove == Color.White ? Zobrist.Black : Zobrist.White;
	}

	private static long RandomUInt64(Random rnd)
	{
		byte[] bytes = new byte[8];
		rnd.nextBytes(bytes);
		return ByteBuffer.wrap(bytes).getLong()&Long.MAX_VALUE;//refactor?
	}
}