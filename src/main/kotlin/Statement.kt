package fr.univ_lille.iut_info

interface Statement

class GroupDeclarationStatement(identifier: String) : Statement
class NodeDeclarationStatement(identifier: String, type: ObjectAlfrType, groups: List<String>) : Statement
class RewriteRuleStatement(pattern: Pattern, condition: Condition, transform: Transform): Statement