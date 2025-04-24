public class Transpositions
{
	public enum ScoreType
	{
		GreaterOrEqual,
		LessOrEqual,
		Exact
	}

	public static class HashEntry
	{
		public long Hash;       //8 Bytes
		public short Score;      //2 Bytes
		public byte Depth;       //1 Byte
		public byte Age;         //1 Byte
		public ScoreType Type;   //1 Byte
		public Move BestMove;    //3 Bytes
		//==================================
		//                        16 Bytes
	}
	public static final int DEFAULT_SIZE_MB = 50;
	private static final int ENTRY_SIZE = 16; //BYTES
	static HashEntry[] _table;

	static boolean Index(long hash, int[] index)
	{
		index[0] = (int)(hash % _table.length);
		if (_table[index[0]].Hash != hash)
			index[0] ^= 1; //try other slot

		if (_table[index[0]].Hash != hash)
			return false; //both slots missed

		//a table hit resets the age
		_table[index[0]].Age = 0;
		return true;
	}

	static int Index(long hash)
	{
		int index = (int)(hash % _table.length);
		HashEntry e0 = _table[index];
		HashEntry e1 = _table[index ^ 1];

		if (e0.Hash == hash)
			return index;

		if (e1.Hash == hash)
			return index ^ 1;

		//raise age of both and choose the older, shallower entry!
		return (++e0.Age - e0.Depth) > (++e1.Age - e1.Depth) ? index : index ^ 1;
	}

	static
	{
		Resize(DEFAULT_SIZE_MB);
	}

	public static void Resize(int hashSizeMBytes)
	{
		int length = (hashSizeMBytes * 1024 * 1024) / ENTRY_SIZE;
		_table = new HashEntry[length];
		Clear();//refactor?
	}

	public static void Clear()
	{
		for(int i=0; i<_table.length; i++)
			_table[i]=new HashEntry();
	}

	public static void Store(long zobristHash, int depth, int ply, SearchWindow window, int score, Move bestMove)
	{
		HashEntry entry = _table[Index(zobristHash)];

		//don't overwrite a bestmove with 'default' unless it's a new position
		if (entry.Hash != zobristHash || bestMove != null)
			entry.BestMove = bestMove;

		entry.Hash = zobristHash;
		entry.Depth = depth < 0 ? 0 : (byte)depth;
		entry.Age = 0;

		//a checkmate score is reduced by the number of plies from the root so that shorter mates are preferred
		//but when we talk about a position being 'mate in X' then X is independent of the root distance. So we store
		//the score relative to the position by adding the current ply to the encoded mate distance (from the root).
		if (Evaluation.IsCheckmate(score))
			score += Integer.signum(score) * ply;

		if (score >= window.Ceiling)
		{
			entry.Type = ScoreType.GreaterOrEqual;
			entry.Score = (short)window.Ceiling;
		}
		else if(score <= window.Floor)
		{
			entry.Type = ScoreType.LessOrEqual;
			entry.Score = (short)window.Floor;
		}
		else
		{
			entry.Type = ScoreType.Exact;
			entry.Score = (short)score;
		}
	}

	public static boolean GetBestMove(Board position, Move[] bestMove)
	{
		int[] index=new int[1];
		bestMove[0] = Index(position.getZobristHash(), index) ? _table[index[0]].BestMove : null;
		return bestMove[0]!=null;
	}

	public static boolean GetScore(long zobristHash, int depth, int ply, SearchWindow window, int[] score)
	{
		score[0] = 0;
		int[] index=new int[1];
		if (!Index(zobristHash, index))
			return false;

		HashEntry entry = _table[index[0]];
		if (entry.Depth < depth)
			return false;

		score[0] = entry.Score;

		//a checkmate score is reduced by the number of plies from the root so that shorter mates are preferred
		//but when we store it in the TT the score is made relative to the current position. So when we want to
		//retrieve the score we have to subtract the current ply to make it relative to the root again.
		if (Evaluation.IsCheckmate(score[0]))
			score[0] -= Integer.signum(score[0]) * ply;

		//1.) score is exact and within window
		if (entry.Type == ScoreType.Exact)
			return true;
		//2.) score is below floor
		if (entry.Type == ScoreType.LessOrEqual && score[0] <= window.Floor)
			return true; //failLow
		//3.) score is above ceiling
        return entry.Type == ScoreType.GreaterOrEqual && score[0] >= window.Ceiling; //failHigh
    }
}