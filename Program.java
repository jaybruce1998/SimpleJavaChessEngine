import java.util.Scanner;

public class Program {
    public static void main(String[] args) {
        Board board = new Board(Board.STARTING_POS_FEN);
        boolean aiTurn = true;
        Scanner input = new Scanner(System.in);
        while (true) {
            if (aiTurn) {
                System.out.println();
                IterativeSearch search = new IterativeSearch(11, board);
                System.out.println("Score: " + search.getScore());
                System.out.println("Nodes: "+search.getNodesVisited());
                board.Play(search.PrincipalVariation()[0]);
                System.out.println("AI played: " + search.PrincipalVariation()[0]);
            } else {
                LegalMoves legalMoves=new LegalMoves(board);
                /*Console.WriteLine("Your turn, legal moves: ");
                for(int i = 0; i < legalMoves.Count; i++)
                {
                    Console.Write($"{i+1}: {legalMoves[i]}, ");
                }
                Console.WriteLine();*/
                Move move = null;
                while (move == null) {
                    try {
                        System.out.print("Your move: ");
                        move = new Move(input.nextLine());
                        if (!legalMoves.contains(move)) {
                            System.out.println("That's illegal!");
                            move = null;
                        }
                    } catch (Exception e) {
                        System.out.println("Invalid move, try again.");
                        move = null;
                    }
                }
                board.Play(move);
            }
            //System.out.println(board);
            aiTurn = !aiTurn;
        }
    }
}