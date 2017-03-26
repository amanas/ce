#! /usr/bin/env python

# PonyGE
# Copyright (c) 2009-2012 Erik Hemberg and James McDermott
# Hereby licensed under the GNU GPL v3.
# http://ponyge.googlecode.com

"""Small GE implementation."""

import sys, copy, re, random, math, operator

class Grammar(object):
    """Context Free Grammar"""
    NT = "NT" # Non Terminal
    T = "T" # Terminal    

    def __init__(self, gram, MAX_WRAPS=0):
        self.rules = {}
        self.non_terminals, self.terminals = set(), set()
        self.start_rule = None
        self.MAX_WRAPS=MAX_WRAPS

        self.read_bnf_file(gram)

    def read_bnf_file(self, gram):
        """Read a grammar file in BNF format"""
        rule_separator = "::="
        # Don't allow space in NTs, and use lookbehind to match "<"
        # and ">" only if not preceded by backslash. Group the whole
        # thing with capturing parentheses so that split() will return
        # all NTs and Ts. TODO does this handle quoted NT symbols?
        non_terminal_pattern = r"((?<!\\)<\S+?(?<!\\)>)"
        # Use lookbehind again to match "|" only if not preceded by
        # backslash. Don't group, so split() will return only the
        # productions, not the separators.
        production_separator = r"(?<!\\)\|"
        
        # Read the grammar file
        for line in gram.splitlines():
            if not line.startswith("#") and line.strip() != "":
                # Split rules. Everything must be on one line
                if line.find(rule_separator):
                    lhs, productions = line.split(rule_separator, 1) # 1 split
                    lhs = lhs.strip()
                    if not re.search(non_terminal_pattern, lhs):
                        raise ValueError("lhs is not a NT:", lhs)
                    self.non_terminals.add(lhs)
                    if self.start_rule == None:
                        self.start_rule = (lhs, self.NT)
                    # Find terminals and non-terminals
                    tmp_productions = []
                    for production in re.split(production_separator, productions):
                        production = production.strip().replace(r"\|", "|")
                        tmp_production = []
                        for symbol in re.split(non_terminal_pattern, production):
                            symbol = symbol.replace(r"\<", "<").replace(r"\>", ">")
                            if len(symbol) == 0:
                                continue
                            elif re.match(non_terminal_pattern, symbol):
                                tmp_production.append((symbol, self.NT))
                            else:
                                self.terminals.add(symbol)
                                tmp_production.append((symbol, self.T))

                        tmp_productions.append(tmp_production)
                    # Create a rule
                    if not lhs in self.rules:
                        self.rules[lhs] = tmp_productions
                    else:
                        raise ValueError("lhs should be unique", lhs)
                else:
                    raise ValueError("Each rule must be on one line")

    def __str__(self):
        return "%s %s %s %s" % (self.terminals, self.non_terminals,
                                self.rules, self.start_rule)

    def generate(self, _input):
        """Map input via rules to output. Returns output and used_input"""
        used_input = 0
        wraps = 0
        output = []
        production_choices = []

        unexpanded_symbols = [self.start_rule]
        while (wraps <= self.MAX_WRAPS) and (len(unexpanded_symbols) > 0):
            # Wrap
            if used_input % len(_input) == 0 and \
                    used_input > 0 and \
                    len(production_choices) > 1:
                wraps += 1
            # Expand a production
            current_symbol = unexpanded_symbols.pop(0)
            # Set output if it is a terminal
            if current_symbol[1] != self.NT:
                output.append(current_symbol[0])
            else:
                production_choices = self.rules[current_symbol[0]]
                # Select a production
                current_production = _input[used_input % len(_input)] % len(production_choices)
                # Use an input if there was more then 1 choice
                if len(production_choices) > 1:
                    used_input += 1
                # Derviation order is left to right(depth-first)
                unexpanded_symbols = production_choices[current_production] + unexpanded_symbols

        #Not completly expanded
        if len(unexpanded_symbols) > 0:
            return (None, used_input)

        return ("".join(output), used_input)
