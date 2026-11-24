/*
 * gMark: A domain- and query language-independent query workload generator and query language utility library.
 * Copyright (C) 2021  Roan Hofland (roan@roanh.dev).  All rights reserved.
 * GitHub Repository: https://github.com/RoanH/gMark
 *
 * gMark is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gMark is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.roanh.gmark.client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import dev.roanh.gmark.cli.client.EvaluatorClient;
import dev.roanh.gmark.eval.DatabaseGraph;
import dev.roanh.gmark.eval.PathQuery;
import dev.roanh.gmark.eval.PropertyGraph;
import dev.roanh.gmark.eval.PropertyGraphEvaluator;
import dev.roanh.gmark.eval.ReachabilityQueryEvaluator;
import dev.roanh.gmark.eval.ResultGraph;
import dev.roanh.gmark.lang.QueryLanguage;
import dev.roanh.gmark.lang.QueryLanguageSyntax;
import dev.roanh.gmark.lang.ReachabilityQueryLanguageSyntax;
import dev.roanh.gmark.lang.gql.GQL;
import dev.roanh.gmark.util.Util;
import dev.roanh.gmark.util.graph.generic.IntGraph;
import dev.roanh.util.Dialog;
import dev.roanh.util.FileSelector;
import dev.roanh.util.FileSelector.FileExtension;

/**
 * Tab where you can evaluate queries on a database graph.
 * @author Roan
 * @see ReachabilityQueryEvaluator
 */
public class EvalTab extends JPanel{
	/**
	 * Serial ID.
	 */
	private static final long serialVersionUID = -3895083891700621666L;
	/**
	 * Extension used for database graph files.
	 */
	private static final FileExtension EDGE_EXT = FileSelector.registerFileExtension("Database Graph Files", "edge");
	/**
	 * Background worker executor.
	 */
	private static final ExecutorService executor = Executors.newSingleThreadExecutor();
	/**
	 * Button to start evaluating the input query.
	 */
	private final JButton run = new JButton("Evaluate Query");
	/**
	 * Label showing basic graph statistics.
	 */
	private final JLabel graphInfo = new JLabel("No graph loaded", SwingConstants.CENTER);
	/**
	 * Text area displaying the query output.
	 */
	private final JTextArea queryOutput = new JTextArea();
	/**
	 * The graph loaded in the tab and its two evaluation views.
	 */
	private LoadedGraph loadedGraph;

	private record LoadedGraph(ReachabilityQueryEvaluator reachability, PropertyGraph properties){
	}

	/**
	 * Constructs a new query evaluation tab.
	 */
	public EvalTab(){
		super(new BorderLayout());
		
		JPanel graph = new JPanel(new GridLayout(1, 2));
		graph.setBorder(BorderFactory.createTitledBorder("Database Graph"));
		JButton loadGraph = new JButton("Load Database Graph");
		loadGraph.addActionListener(this::loadGraph);
		graph.add(loadGraph);
		graph.add(graphInfo);
			
		JPanel input = new JPanel(new BorderLayout());
		input.add(graph, BorderLayout.PAGE_START);
		input.add(createQueryPanel(run), BorderLayout.CENTER);
		
		JPanel output = new JPanel(new BorderLayout());
		output.setBorder(BorderFactory.createTitledBorder("Query Output"));
		output.add(run, BorderLayout.PAGE_START);
		queryOutput.setEditable(false);
		output.add(new JScrollPane(queryOutput), BorderLayout.CENTER);
		
		this.add(input, BorderLayout.PAGE_START);
		this.add(output, BorderLayout.CENTER);
	}
	
	/**
	 * Creates the query input panel.
	 * @param run The query run button to configure the run listener for.
	 * @return A panel to input a query.
	 */
	private JPanel createQueryPanel(JButton run){
		JPanel sourcePanel = new JPanel(new GridLayout(2, 1));
		sourcePanel.add(new JLabel("Source (-1 for any)", SwingConstants.CENTER));
		JSpinner source = new JSpinner(new SpinnerNumberModel(-1, -1, Integer.MAX_VALUE, 1));
		sourcePanel.add(source);
		
		JPanel queryPanel = new JPanel(new GridLayout(3, 1));
		JTextField query = new JTextField();
		JPanel queryButtons = new JPanel();
		queryButtons.setLayout(new BoxLayout(queryButtons, BoxLayout.X_AXIS));
		queryPanel.add(queryButtons);
		queryPanel.add(query);

		JPanel targetPanel = new JPanel(new GridLayout(2, 1));
		targetPanel.add(new JLabel("Target (-1 for any)", SwingConstants.CENTER));
		JSpinner target = new JSpinner(new SpinnerNumberModel(-1, -1, Integer.MAX_VALUE, 1));
		targetPanel.add(target);
		
		JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
		JComboBox<QueryLanguage> lang = new JComboBox<QueryLanguage>(QueryLanguage.stream()
			.filter(language->language.isReachabilityQueryLanguage() || language == QueryLanguage.PGQ || language == QueryLanguage.GQL)
			.toArray(QueryLanguage[]::new));
		updateQueryEditorButtons(queryButtons, query, lang.getItemAt(lang.getSelectedIndex()));
		lang.addActionListener(_->{
			QueryLanguage language = lang.getItemAt(lang.getSelectedIndex());
			updateQueryEditorButtons(queryButtons, query, language);
			source.setEnabled(language.isReachabilityQueryLanguage());
			target.setEnabled(language.isReachabilityQueryLanguage());
		});
		JLabel langLabel = new JLabel("Language");
		langLabel.setLabelFor(lang);
		langPanel.add(langLabel);
		langPanel.add(lang);
		queryPanel.add(langPanel);
		
		run.addActionListener(_->{
			QueryLanguage language = lang.getItemAt(lang.getSelectedIndex());
			if(language == QueryLanguage.PGQ){
				queryOutput.setText(language + " query evaluation is not implemented yet.");
				return;
			}

			try{
				LoadedGraph graph = loadedGraph;
				if(graph == null){
					Dialog.showMessageDialog("Please load a database graph first.");
					return;
				}
				
				if(query.getText().isBlank()){
					Dialog.showMessageDialog("No query provided.");
					return;
				}

				if(language == QueryLanguage.GQL){
					GQL parsed = (GQL)language.parse(query.getText());
					runQuery(()->{
						long start = System.nanoTime();
						var result = PropertyGraphEvaluator.evaluate(graph.properties(), parsed);
						long time = System.nanoTime() - start;
						StringWriter buffer = new StringWriter();
						EvaluatorClient.printQueryResult(parsed, result, time, new PrintWriter(buffer));
						return buffer.toString();
					});
				}else{
					PathQuery parsed = PathQuery.of(
						(int)source.getValue(),
						(ReachabilityQueryLanguageSyntax)language.parse(query.getText(), graph.reachability().getLabels()),
						(int)target.getValue()
					);
					runQuery(()->{
						long start = System.nanoTime();
						ResultGraph result = graph.reachability().evaluate(parsed);
						long time = System.nanoTime() - start;
						StringWriter buffer = new StringWriter();
						EvaluatorClient.printQueryResult(parsed, result, time, new PrintWriter(buffer));
						return buffer.toString();
					});
				}
			}catch(Exception e1){
				e1.printStackTrace();
				Dialog.showErrorDialog("Failed to parse query: " + e1.getMessage());
			}
		});

		JPanel line = new JPanel();
		line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));
		line.setBorder(BorderFactory.createTitledBorder("Query"));
		line.add(sourcePanel);
		line.add(Box.createHorizontalStrut(2));
		line.add(queryPanel);
		line.add(Box.createHorizontalStrut(2));
		line.add(targetPanel);
		return line;
	}
	
	/**
	 * Shows the input shortcuts for the selected query language.
	 * @param buttons The panel holding the shortcut buttons.
	 * @param query The query text field to insert shortcuts in.
	 * @param language The currently selected query language.
	 */
	private void updateQueryEditorButtons(JPanel buttons, JTextField query, QueryLanguage language){
		List<String> shortcuts = switch(language){
			case PGQ -> List.of(QueryLanguageSyntax.GRAPH_TABLE, " " + QueryLanguageSyntax.MATCH,
				QueryLanguageSyntax.COLUMNS, "(", ")", QueryLanguageSyntax.FULL_EDGE_LEFT,
				QueryLanguageSyntax.FULL_EDGE_RIGHT, ":", ",");
			case GQL -> List.of(QueryLanguageSyntax.MATCH, QueryLanguageSyntax.RETURN, "(", ")",
				QueryLanguageSyntax.FULL_EDGE_LEFT, QueryLanguageSyntax.FULL_EDGE_RIGHT, ":", ",");
			default -> List.of(
				String.valueOf(QueryLanguageSyntax.CHAR_DISJUNCTION),
				String.valueOf(QueryLanguageSyntax.CHAR_INTERSECTION),
				String.valueOf(QueryLanguageSyntax.CHAR_JOIN),
				String.valueOf(QueryLanguageSyntax.CHAR_INVERSE),
				String.valueOf(QueryLanguageSyntax.CHAR_KLEENE),
				"id", "(", ")"
			);
		};

		buttons.removeAll();
		for(String shortcut : shortcuts){
			JButton button = new JButton(shortcut.strip());
			button.addActionListener(_->{
				try{
					int caret = query.getCaretPosition();
					query.getDocument().insertString(caret, shortcut, null);
					int brackets = shortcut.indexOf("[]");
					query.setCaretPosition(caret + (brackets < 0 ? shortcut.length() : brackets + 1));
					query.requestFocusInWindow();
				}catch(BadLocationException ignore){
				}
			});
			
			buttons.add(button);
		}
		
		buttons.revalidate();
		buttons.repaint();
	}
	
	/**
	 * Runs the given query.
	 * @param evaluate The evaluation that returns the formatted output.
	 */
	private void runQuery(Supplier<String> evaluate){
		run.setEnabled(false);
		queryOutput.setText("Running query...");
		executor.execute(()->{
			try{
				String output = evaluate.get();
				SwingUtilities.invokeLater(()->{
					queryOutput.setText(output);
					queryOutput.setCaretPosition(0);
				});
			}catch(Exception e){
				e.printStackTrace();
				SwingUtilities.invokeLater(()->{
					queryOutput.setText("Failed to evaluate the query: " + e.getMessage());
					Dialog.showErrorDialog(queryOutput.getText());
				});
			}finally{
				SwingUtilities.invokeLater(()->run.setEnabled(true));
			}
		});
	}
	
	/**
	 * Asks the query for a graph to load in the evaluator.
	 * @param event The button click event.
	 */
	private void loadGraph(ActionEvent event){
		Path file = Dialog.showFileOpenDialog(EDGE_EXT);
		if(file != null){
			JButton button = ((JButton)event.getSource());
			button.setEnabled(false);
			executor.execute(()->{
				try{
					IntGraph data = Util.readGraph(file);
					if(data == null){
						throw new IOException("The graph file is empty.");
					}
					DatabaseGraph db = new DatabaseGraph(data);
					LoadedGraph graph = new LoadedGraph(new ReachabilityQueryEvaluator(db), PropertyGraph.fromIntGraph(data));
					SwingUtilities.invokeLater(()->{
						loadedGraph = graph;
						graphInfo.setText("Vertices: %d, Edges: %d (%d unique), Labels: %d".formatted(data.getVertexCount(), data.getEdgeCount(), db.getEdgeCount(), data.getLabelCount()));
					});
				}catch(IOException | IllegalArgumentException e){
					SwingUtilities.invokeLater(()->Dialog.showErrorDialog("Failed to read the database graph: " + e.getMessage()));
				}finally{
					SwingUtilities.invokeLater(()->button.setEnabled(true));
				}
			});
		}
	}
	
}
