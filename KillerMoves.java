import java.util.Arrays;

public class KillerMoves
{
	Move[] _moves;
	int _depth;
	int _width;

	public KillerMoves(int width)
	{
		_moves = new Move[0];
		_depth = 0;
		_width = width;
	}

	public void Expand(int depth)
	{
		_depth = Math.max(_depth, depth);
		_moves = Arrays.copyOf(_moves, _depth * _width);//refactor?
	}

	public void Add(Move move, int depth)
	{
		int index0 = _width * (_depth - depth);
		//We shift all moves by one slot to make room but overwrite a potential duplicate of 'move' then store the new 'move' at [0]
		int last = index0;
		for (; last < index0 + _width - 1; last++)
			if (_moves[last]!=null&&_moves[last].equals(move)) //if 'move' is present, we want to overwrite it instead of the one at [_width-1]
				break;
		//2. start with the last slot and 'save' the previous values until the first slot got duplicated
		for (int index = last; index >= index0; index--)
			_moves[index] = _moves[index - 1];
		//3. store new 'move' in the first slot
		_moves[index0] = move;
	}

	public Move[] Get(int depth)
	{
		Move[] line = new Move[_width];
		int index0 = _width * (_depth - depth);
		System.arraycopy(_moves, index0, line, 0, _width);
		return line;
	}

	public boolean Contains(int depth, Move move)
	{
		for(int index0 = _width * (_depth - depth), i=index0, m=i+_width; i<m; i++)//refactor?
			if(_moves[i]!=null&&move.equals(_moves[i]))
				return true;
		return false;
	}
}