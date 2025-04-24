import java.util.ArrayList;
import java.util.List;

public class Playmaker
{
	//REFACTOR - don't use Lists?
	public static Iterable<MoveBoard> Play(Board position, int depth, KillerMoves killers, History history)
	{
		List<MoveBoard> r=new ArrayList<>();
		Move[] bm=new Move[1];
		Move bestMove;
		if(Transpositions.GetBestMove(position, bm)) {
			bestMove=bm[0];
			r.add(new MoveBoard(bestMove, new Board(position, bestMove)));
		}

		//2. Captures Mvv-Lva, PV excluded
		for (var capture: MoveList.SortedCaptures(position))
		{
			var nextPosition = new Board(position, capture);
			if (!nextPosition.IsChecked(position.getSideToMove()))
				r.add(new MoveBoard(capture, nextPosition));
				//yield return new MoveBoard(capture, nextPosition);
		}

		//3. Killers if available
		for (Move killer: killers.Get(depth))
			if (killer!=null && position.get(killer.ToSquare) == Piece.None && position.IsPlayable(killer))//refactor?
			{
				var nextPosition = new Board(position, killer);
				if (!nextPosition.IsChecked(position.getSideToMove()))
					r.add(new MoveBoard(killer, nextPosition));
					//yield return new MoveBoard(killer, nextPosition);
			}

		//4. Play quiet moves that aren't known killers
		for (var move: MoveList.SortedQuiets(position, history))
			if (!killers.Contains(depth, move))
			{
				var nextPosition = new Board(position, move);
				if (!nextPosition.IsChecked(position.getSideToMove()))
					r.add(new MoveBoard(move, nextPosition));
					//yield return new MoveBoard(move, nextPosition);
			}
		return r;
	}

	static Iterable<Board> Play(Board position)
	{
		List<Board> r=new ArrayList<>();
		for (var capture: MoveList.SortedCaptures(position))
		{
			var nextPosition = new Board(position, capture);
			if (!nextPosition.IsChecked(position.getSideToMove()))
				r.add(nextPosition);
				//yield return nextPosition;
		}

		for (var move: MoveList.Quiets(position))
		{
			var nextPosition = new Board(position, move);
			if (!nextPosition.IsChecked(position.getSideToMove()))
				r.add(nextPosition);
				//yield return nextPosition;
		}
		return r;
	}

	static Iterable<Board> PlayCaptures(Board position)
	{
		List<Board> r=new ArrayList<>();
		for (var capture: MoveList.SortedCaptures(position))
		{
			var nextPosition = new Board(position, capture);
			if (!nextPosition.IsChecked(position.getSideToMove()))
				r.add(nextPosition);
				//yield return nextPosition;
		}
		return r;
	}

	static Board PlayNullMove(Board position)
	{
		Board copy = new Board(position);
		copy.PlayNullMove();
		return copy;
	}
}