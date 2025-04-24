public class History
{
	private static final int Squares = 64;
	private static final  int Pieces = 12;
	private final int[][] Positive = new int[Squares][Pieces];
	private final int[][] Negative = new int[Squares][Pieces];
	
	public void Scale()
	{
		for (int square = 0; square < Squares; square++)
			for(int piece = 0; piece < Pieces; piece++)
			{
				Positive[square][piece] /= 2;
				Negative[square][piece] /= 2;
			}
	}

	private int PieceIndex(Piece piece)
	{
		return (piece.value >> 1) - 2; //BlackPawn = 0...
	}

	public void Good(Board context, Move move, int depth)
	{
		int iPiece = PieceIndex(context.get(move.FromSquare));
		Positive[move.ToSquare][iPiece] += depth * depth;
	}

	public void Bad(Board context, Move move, int depth)
	{
		int iPiece = PieceIndex(context.get(move.FromSquare));
		Negative[move.ToSquare][iPiece] += depth * depth;
	}

	public float Value(Board context, Move move)
	{
		int iPiece = PieceIndex(context.get(move.FromSquare));
		float a = Positive[move.ToSquare][iPiece];
		float b = Negative[move.ToSquare][iPiece];
		return a / (a + b + 1);//ratio of good increments in the range of [0..1]
	}
}